package com.iread.backend.readingfeature.service;

import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.ReadingFeatureRepository;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentFeatureProfileService {

    public static final String ANALYSIS_VERSION = "WEAKNESS_V1";
    private static final double FIXATION_DURATION_THRESHOLD_MS = 1_200.0;
    private static final double FIXATION_COUNT_THRESHOLD = 3.0;
    private static final double REGRESSION_COUNT_THRESHOLD = 2.0;
    private static final double READING_TIME_THRESHOLD_MS = 2_500.0;

    private final TrainingRepository trainingRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final ReadingFeatureRepository readingFeatureRepository;
    private final StudentFeatureProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public List<StudentFeatureProfileView> getProfiles(Long studentId) {
        return profileRepository.findAllByStudentIdOrderByWeaknessScoreDesc(studentId).stream()
                .map(StudentFeatureProfileView::from)
                .toList();
    }

    @Transactional
    public synchronized List<StudentFeatureProfileView> recalculate(StudentEntity student) {
        Map<String, List<Evidence>> evidenceByFeature = collectEvidence(student.getId());
        if (evidenceByFeature.isEmpty()) {
            return getProfiles(student.getId());
        }

        Map<String, ReadingFeatureEntity> features = new HashMap<>();
        readingFeatureRepository.findAllByFeatureCodeIn(evidenceByFeature.keySet())
                .forEach(feature -> features.put(feature.getFeatureCode(), feature));

        long[] nextId = {profileRepository.findMaxId() + 1};
        LocalDateTime analyzedAt = LocalDateTime.now();
        List<StudentFeatureProfileEntity> updated = new ArrayList<>();
        for (Map.Entry<String, List<Evidence>> entry : evidenceByFeature.entrySet()) {
            ReadingFeatureEntity feature = features.get(entry.getKey());
            if (feature == null) {
                continue;
            }
            StudentFeatureProfileEntity profile = profileRepository
                    .findByStudentIdAndReadingFeatureId(student.getId(), feature.getId())
                    .orElseGet(() -> new StudentFeatureProfileEntity(
                            nextId[0]++, student, feature, BigDecimal.ZERO.setScale(4)
                    ));
            Metrics metrics = calculate(entry.getValue(), analyzedAt);
            profile.updateMetrics(
                    metrics.accuracyRate(),
                    metrics.avgPronunciationScore(),
                    metrics.pronunciationErrorRate(),
                    metrics.avgFixationDurationMs(),
                    metrics.avgFixationCount(),
                    metrics.avgRegressionCount(),
                    metrics.skipRate(),
                    metrics.avgReadingTimeMs(),
                    metrics.weaknessScore(),
                    metrics.confidence(),
                    metrics.evidenceCount(),
                    metrics.lastEvidenceAt(),
                    metrics.analyzedAt()
            );
            updated.add(profile);
        }
        profileRepository.saveAll(updated);
        return updated.stream()
                .sorted((left, right) -> Integer.compare(
                        right.getWeaknessScore(), left.getWeaknessScore()
                ))
                .map(StudentFeatureProfileView::from)
                .toList();
    }

    private Map<String, List<Evidence>> collectEvidence(Long studentId) {
        Map<String, List<Evidence>> evidenceByFeature = new LinkedHashMap<>();
        List<TrainingEntity> trainings = trainingRepository
                .findAllByDailyCurriculumStudentIdAndStatus(studentId, TrainingStatus.COMPLETED);
        for (TrainingEntity training : trainings) {
            JsonNode result = parse(training.getResult());
            JsonNode questions = trainingDataRepository.findByTrainingId(training.getId())
                    .map(TrainingDataEntity::getGeneratedData)
                    .map(this::parse)
                    .map(root -> root.path("questions"))
                    .orElse(null);
            if (result == null || questions == null || !questions.isArray()) {
                continue;
            }
            collectTrainingEvidence(result.path("wordAttempts"), questions, evidenceByFeature);
        }
        return evidenceByFeature;
    }

    private void collectTrainingEvidence(
            JsonNode attempts,
            JsonNode questions,
            Map<String, List<Evidence>> evidenceByFeature
    ) {
        if (!attempts.isArray()) {
            return;
        }
        Map<Long, WordAttemptLogEntity> logs = loadLogs(attempts);
        for (JsonNode attempt : attempts) {
            if (!attempt.path("isFinal").asBoolean(false)) {
                continue;
            }
            WordAttemptLogEntity log = logs.get(attempt.path("wordAttemptLogId").asLong(-1));
            if (log == null) {
                continue;
            }
            JsonNode question = findQuestion(questions, attempt.path("questionNo").asInt(-1));
            JsonNode target = findTarget(question, attempt);
            JsonNode featureCodes = target == null ? null : target.path("featureCodes");
            if (featureCodes == null || !featureCodes.isArray()) {
                continue;
            }
            Evidence evidence = Evidence.from(log, attempt);
            for (JsonNode codeNode : featureCodes) {
                String code = codeNode.asText();
                if (!code.isBlank()) {
                    evidenceByFeature.computeIfAbsent(code, ignored -> new ArrayList<>())
                            .add(evidence);
                }
            }
        }
    }

    private Map<Long, WordAttemptLogEntity> loadLogs(JsonNode attempts) {
        List<Long> ids = new ArrayList<>();
        attempts.forEach(attempt -> {
            if (attempt.path("isFinal").asBoolean(false)
                    && attempt.hasNonNull("wordAttemptLogId")) {
                ids.add(attempt.path("wordAttemptLogId").asLong());
            }
        });
        Map<Long, WordAttemptLogEntity> result = new HashMap<>();
        wordAttemptLogRepository.findAllById(ids)
                .forEach(log -> result.put(log.getId(), log));
        return result;
    }

    private JsonNode findQuestion(JsonNode questions, int questionNo) {
        for (JsonNode question : questions) {
            if (question.path("questionNo").asInt(-1) == questionNo) {
                return question;
            }
        }
        return null;
    }

    private JsonNode findTarget(JsonNode question, JsonNode attempt) {
        if (question == null) {
            return null;
        }
        if (attempt.hasNonNull("tokenIndex")) {
            int tokenIndex = attempt.path("tokenIndex").asInt(-1);
            JsonNode words = question.path("words");
            if (words.isArray() && tokenIndex >= 0 && tokenIndex < words.size()) {
                return words.get(tokenIndex);
            }
        }
        int targetIndex = attempt.path("targetIndex").asInt(-1);
        JsonNode targets = question.path("analysisTargets");
        return targets.isArray() && targetIndex >= 0 && targetIndex < targets.size()
                ? targets.get(targetIndex) : null;
    }

    private Metrics calculate(List<Evidence> evidence, LocalDateTime analyzedAt) {
        int count = evidence.size();
        BigDecimal accuracyRate = decimal(average(evidence, item -> item.correct() ? 1 : 0), 4);

        List<Evidence> pronunciation = evidence.stream()
                .filter(item -> item.pronunciationScore() != null)
                .toList();
        Integer avgPronunciationScore = pronunciation.isEmpty() ? null
                : (int) Math.round(average(pronunciation, item -> item.pronunciationScore() * 10.0));
        BigDecimal pronunciationErrorRate = pronunciation.isEmpty() ? null
                : decimal(average(pronunciation, item -> item.pronunciationError() ? 1 : 0), 2);

        List<Evidence> gaze = evidence.stream().filter(Evidence::hasGaze).toList();
        Integer avgFixationDurationMs = nullableRoundedAverage(gaze, Evidence::fixationDurationMs);
        BigDecimal avgFixationCount = nullableDecimalAverage(gaze, Evidence::fixationCount);
        BigDecimal avgRegressionCount = nullableDecimalAverage(gaze, Evidence::regressionCount);

        BigDecimal skipRate = decimal(average(evidence, item -> item.skipped() ? 1 : 0), 2);
        List<Evidence> reading = evidence.stream().filter(item -> item.readingTimeMs() != null).toList();
        Integer avgReadingTimeMs = nullableRoundedAverage(reading, Evidence::readingTimeMs);

        double accuracyError = 1 - accuracyRate.doubleValue();
        double pronunciationError = pronunciation.isEmpty() ? 0
                : average(pronunciation, item ->
                (1 - item.pronunciationScore() / 100.0) * item.analysisConfidence());
        double gazeBurden = gaze.isEmpty() ? 0 : average(gaze, this::gazeBurden);
        double delayOrSkip = average(evidence, item -> {
            double delay = item.readingTimeMs() == null ? 0
                    : cap(item.readingTimeMs() / READING_TIME_THRESHOLD_MS);
            return Math.max(delay, item.skipped() ? 1 : 0);
        });
        int weaknessScore = (int) Math.round(1_000 * cap(
                accuracyError * 0.40
                        + pronunciationError * 0.30
                        + gazeBurden * 0.20
                        + delayOrSkip * 0.10
        ));
        double averageConfidence = average(evidence, Evidence::analysisConfidence);
        BigDecimal confidence = decimal(Math.min(1, count / 10.0) * averageConfidence, 4);
        LocalDateTime lastEvidenceAt = evidence.stream()
                .map(Evidence::createdAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(analyzedAt);

        return new Metrics(
                accuracyRate, avgPronunciationScore, pronunciationErrorRate,
                avgFixationDurationMs, avgFixationCount, avgRegressionCount,
                skipRate, avgReadingTimeMs, weaknessScore, confidence, count,
                lastEvidenceAt, analyzedAt
        );
    }

    private double gazeBurden(Evidence item) {
        double duration = item.fixationDurationMs() == null ? 0
                : cap(item.fixationDurationMs() / FIXATION_DURATION_THRESHOLD_MS);
        double count = item.fixationCount() == null ? 0
                : cap(item.fixationCount() / FIXATION_COUNT_THRESHOLD);
        double regression = item.regressionCount() == null ? 0
                : cap(item.regressionCount() / REGRESSION_COUNT_THRESHOLD);
        return (duration + count + regression) / 3.0;
    }

    private Integer nullableRoundedAverage(
            List<Evidence> evidence,
            ToDoubleFunction<Evidence> mapper
    ) {
        return evidence.isEmpty() ? null : (int) Math.round(average(evidence, mapper));
    }

    private BigDecimal nullableDecimalAverage(
            List<Evidence> evidence,
            ToDoubleFunction<Evidence> mapper
    ) {
        return evidence.isEmpty() ? null : decimal(average(evidence, mapper), 2);
    }

    private double average(List<Evidence> evidence, ToDoubleFunction<Evidence> mapper) {
        return evidence.stream().mapToDouble(mapper).average().orElse(0);
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private double cap(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return null;
        }
    }

    public enum ProfileStatus {
        NORMAL, WATCH, WEAK, CRITICAL;

        static ProfileStatus from(int score) {
            if (score < 400) return NORMAL;
            if (score < 600) return WATCH;
            if (score < 800) return WEAK;
            return CRITICAL;
        }
    }

    public record StudentFeatureProfileView(
            String featureCode,
            double accuracyRate,
            Double avgPronunciationScore,
            Integer avgFixationDurationMs,
            BigDecimal avgFixationCount,
            BigDecimal avgRegressionCount,
            double skipRate,
            Integer avgReadingTimeMs,
            double weaknessScore,
            double confidence,
            int evidenceCount,
            ProfileStatus status,
            String analysisVersion,
            LocalDateTime analyzedAt
    ) {
        static StudentFeatureProfileView from(StudentFeatureProfileEntity profile) {
            int weakness = Objects.requireNonNullElse(profile.getWeaknessScore(), 0);
            return new StudentFeatureProfileView(
                    profile.getReadingFeature().getFeatureCode(),
                    decimalValue(profile.getAccuracyRate()),
                    profile.getAvgPronunciationScore() == null
                            ? null : profile.getAvgPronunciationScore() / 10.0,
                    profile.getAvgFixationDurationMs(),
                    profile.getAvgFixationCount(),
                    profile.getAvgRegressionCount(),
                    decimalValue(profile.getSkipRate()),
                    profile.getAvgReadingTimeMs(),
                    weakness / 1_000.0,
                    decimalValue(profile.getConfidence()),
                    Objects.requireNonNullElse(profile.getEvidenceCount(), 0),
                    ProfileStatus.from(weakness),
                    ANALYSIS_VERSION,
                    profile.getAnalyzedAt()
            );
        }

        private static double decimalValue(BigDecimal value) {
            return value == null ? 0 : value.doubleValue();
        }
    }

    private record Evidence(
            boolean correct,
            boolean pronunciationError,
            Double pronunciationScore,
            double analysisConfidence,
            Integer fixationDurationMs,
            Integer fixationCount,
            Integer regressionCount,
            boolean skipped,
            Integer readingTimeMs,
            LocalDateTime createdAt
    ) {
        static Evidence from(WordAttemptLogEntity log, JsonNode attempt) {
            Double pronunciationScore = attempt.hasNonNull("pronunciationScore")
                    ? attempt.path("pronunciationScore").asDouble() : null;
            double confidence = attempt.hasNonNull("pronunciationConfidence")
                    ? attempt.path("pronunciationConfidence").asDouble() : 1.0;
            return new Evidence(
                    Boolean.TRUE.equals(log.getCorrect()),
                    !"NONE".equals(attempt.path("pronunciationErrorType").asText("NONE")),
                    pronunciationScore,
                    Math.max(0, Math.min(1, confidence)),
                    log.getFixationDurationMs(),
                    log.getFixationCount(),
                    log.getRegressionCount(),
                    Boolean.TRUE.equals(log.getSkipped()),
                    attempt.hasNonNull("wordReadTimeMs")
                            ? attempt.path("wordReadTimeMs").asInt() : null,
                    log.getCreatedAt()
            );
        }

        boolean hasGaze() {
            return fixationDurationMs != null || fixationCount != null || regressionCount != null;
        }
    }

    private record Metrics(
            BigDecimal accuracyRate,
            Integer avgPronunciationScore,
            BigDecimal pronunciationErrorRate,
            Integer avgFixationDurationMs,
            BigDecimal avgFixationCount,
            BigDecimal avgRegressionCount,
            BigDecimal skipRate,
            Integer avgReadingTimeMs,
            Integer weaknessScore,
            BigDecimal confidence,
            Integer evidenceCount,
            LocalDateTime lastEvidenceAt,
            LocalDateTime analyzedAt
    ) {
    }
}
