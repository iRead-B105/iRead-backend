package com.iread.backend.training.admin.service;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.analysis.GazeWordAnalysisAdapter;
import com.iread.backend.gaze.analysis.GazeWordAnalysisRequest;
import com.iread.backend.gaze.analysis.GazeWordAnalysisResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.readingfeature.service.StudentFeatureProfileService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.training.domain.*;
import com.iread.backend.training.curriculum.PersonalizedCurriculumPlanner;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.training.admin.dto.req.ExpectedWordRequest;
import com.iread.backend.training.admin.dto.req.UpdateCurriculumRequest;
import com.iread.backend.training.admin.dto.res.*;
import com.iread.backend.training.repository.*;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {
    private final StudentRepository studentRepository;
    private final DailyCurriculumRepository dailyCurriculumRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingTemplateRepository trainingTemplateRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final WordRepository wordRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final WordAttemptScoreCalculator wordAttemptScoreCalculator;
    private final GazeWordAnalysisAdapter gazeWordAnalysisAdapter;
    private final AiClient aiClient;
    private final PersonalizedTrainingGenerationService personalizedTrainingGenerationService;
    private final StudentFeatureProfileService studentFeatureProfileService;
    private final PersonalizedCurriculumPlanner personalizedCurriculumPlanner;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final ObjectMapper objectMapper;

    public List<CurriculumLogResponse> getCurriculumLogs(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to
    ) {
        validateStudentOwner(teacherId, studentId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTimeExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        return dailyCurriculumRepository.findCompletedByStudentIdWithin(
                        studentId,
                        fromDateTime,
                        toDateTimeExclusive
                )
                .stream().map(curriculum -> new CurriculumLogResponse(
                        curriculum.getId(), curriculum.getCompletedAt().toLocalDate(), averageAccuracy(curriculum),
                        curriculum.getTrainings().stream().map(this::toCurriculumLogItem).toList()
                )).toList();
    }

    public TrainingLogResponse getTrainingLog(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        return new TrainingLogResponse(curriculum.getTrainings().stream().map(training ->
                new TrainingLogResponse.TrainingItem(
                        training.getId(), training.getTrainingTemplate().getName(), training.getStartedAt(),
                        training.getFinishedAt(), parseQuestionResults(training.getResult()),
                        training.getAccuracy(), parseIncorrectItems(training.getResult())
                )).toList());
    }

    public TrainingStatisticsResponse getStatistics(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        List<TrainingStatisticsResponse.TrainingItem> items = curriculum.getTrainings().stream().map(current -> {
            TrainingEntity previous = previousTraining(studentId, current);
            return new TrainingStatisticsResponse.TrainingItem(
                    current.getId(), current.getTrainingTemplate().getName(), dateOf(current), current.getAccuracy(),
                    previous == null ? null : dateOf(previous), previous == null ? null : previous.getAccuracy()
            );
        }).toList();
        return new TrainingStatisticsResponse(items);
    }

    public List<TrainingCatalogResponse> getTrainingCatalog(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        Map<Long, BigDecimal> achievements = trainingRepository
                .findAllByDailyCurriculumStudentIdAndStatus(studentId, TrainingStatus.COMPLETED)
                .stream()
                .filter(training -> training.getAccuracy() != null)
                .collect(Collectors.toMap(
                        training -> training.getTrainingTemplate().getId(),
                        TrainingEntity::getAccuracy,
                        BigDecimal::max
                ));
        return trainingTemplateRepository.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc().stream()
                .map(template -> new TrainingCatalogResponse(
                        template.getId(), template.getCurriculumUnit().getUnitName(), template.getSequenceNo(),
                        template.getName(), achievements.get(template.getId())
                )).toList();
    }

    public DailyCurriculumResponse getDailyCurriculum(Long teacherId, Long studentId, Long curriculumId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = findCurriculum(studentId, curriculumId);
        return toDailyCurriculumResponse(curriculum);
    }

    public DailyCurriculumResponse getCurrentDailyCurriculum(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findByStudentIdAndStatus(studentId, DailyCurriculumStatus.NOT_STARTED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NEXT_CURRICULUM_NOT_FOUND",
                        "수정 가능한 커리큘럼을 찾을 수 없습니다."
                ));
        return getDailyCurriculum(teacherId, studentId, curriculum.getId());
    }

    @Transactional
    public DailyCurriculumResponse createDailyCurriculum(
            Long teacherId,
            Long studentId,
            UpdateCurriculumRequest request
    ) {
        StudentEntity student = studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        if (dailyCurriculumRepository
                .findByStudentIdAndStatus(studentId, DailyCurriculumStatus.NOT_STARTED)
                .isPresent()) {
            throw new ConflictException("수정 가능한 커리큘럼은 한 개만 생성할 수 있습니다.");
        }

        List<Long> ids = request.trainingTemplateIds();
        List<TrainingTemplateEntity> templates = resolveTemplates(ids);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository.saveAndFlush(
                new DailyCurriculumEntity(student, templates)
        );
        return toDailyCurriculumResponse(curriculum);
    }

    @Transactional
    public void updateDailyCurriculum(Long teacherId, Long studentId, Long curriculumId,
        UpdateCurriculumRequest request) {
        validateStudentOwner(teacherId, studentId);
        DailyCurriculumEntity curriculum = dailyCurriculumRepository
                .findForUpdate(curriculumId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "일일 커리큘럼을 찾을 수 없습니다."
                ));
        if (curriculum.getTrainings().stream().anyMatch(training -> !training.isEditable())) {
            throw new ConflictException("훈련 자료 생성이 완료된 커리큘럼은 수정할 수 없습니다.");
        }
        List<Long> ids = request.trainingTemplateIds();
        List<TrainingTemplateEntity> templates = resolveTemplates(ids);

        curriculum.getTrainings().forEach(t -> trainingDataRepository.deleteByTrainingId(t.getId()));
        trainingDataRepository.flush();
        curriculum.getTrainings().clear();
        dailyCurriculumRepository.flush();
        curriculum.replaceTrainings(templates);
        dailyCurriculumRepository.flush();
    }

    public List<ExpectedWordResponse> getExpectedWords(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        ObjectNode root = trainingDataRoot(training, false);
        if (root == null) return List.of();
        List<ExpectedWordResponse> result = new ArrayList<>();
        root.withArray("expectedWords").forEach(node -> result.add(new ExpectedWordResponse(
                node.path("wordId").asLong(), node.path("wordName").asText())));
        return result;
    }

    public TrainingDetailResponse getTrainingDetail(
            Long teacherId,
            Long studentId,
            Long trainingId
    ) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        JsonNode generatedData = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(this::parseNullableObject)
                .orElse(null);
        return new TrainingDetailResponse(
                training.getId(),
                training.getTrainingTemplate().getId(),
                training.getTrainingTemplate().getName(),
                parseObject(training.getTrainingTemplate().getPrompt()),
                generatedData,
                training.getStatus().name().toLowerCase(Locale.ROOT),
                training.getStartedAt(),
                training.getFinishedAt(),
                parseNullableObject(training.getResult()),
                training.getAccuracy()
        );
    }

    public TrainingExportFile exportTraining(
            Long teacherId,
            Long studentId,
            Long trainingId,
            String format
    ) {
        String normalizedFormat = format == null
                ? "" : format.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("json", "csv").contains(normalizedFormat)) {
            throw new IllegalArgumentException("내보내기 형식은 csv 또는 json이어야 합니다.");
        }
        TrainingDetailResponse detail = getTrainingDetail(teacherId, studentId, trainingId);
        byte[] content = normalizedFormat.equals("json")
                ? writeBytes(detail)
                : toCsv(detail).getBytes(StandardCharsets.UTF_8);
        return new TrainingExportFile(
                "training-" + trainingId + "." + normalizedFormat,
                content
        );
    }

    @Transactional
    public JsonNode generateTraining(Long teacherId, Long studentId, Long trainingId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        if (!training.isEditable()) {
            throw new ConflictException("시작했거나 완료한 훈련은 다시 생성할 수 없습니다.");
        }

        TrainingDataEntity data = findOrCreateTrainingData(training);
        ObjectNode generatedData = personalizedTrainingGenerationService.generate(training);
        data.updateGeneratedData(writeJson(generatedData));
        training.markReady();
        return generatedData;
    }

    @Transactional
    public BigDecimal completeTraining(
            Long teacherId,
            Long studentId,
            Long trainingId,
            JsonNode result,
            LocalDateTime completedAt
    ) {
        StudentEntity student = validateStudentOwner(teacherId, studentId);
        TrainingEntity training = trainingRepository.findForUpdate(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));

        if (training.isCompleted()) {
            return training.getAccuracy();
        }
        if (!training.isCompletable()) {
            throw new ConflictException("준비되지 않은 훈련은 완료할 수 없습니다.");
        }
        if (result == null || !result.isObject()) {
            throw new IllegalArgumentException("훈련 결과는 JSON 객체여야 합니다.");
        }
        trainingInputRequirementService.validateCompletion(trainingId);
        ObjectNode finalResult = (ObjectNode) result.deepCopy();
        if (training.getResult() != null && !training.getResult().isBlank()) {
            JsonNode progressAttempts = parseObject(training.getResult()).path("wordAttempts");
            if (progressAttempts.isArray()) {
                finalResult.set("wordAttempts", progressAttempts.deepCopy());
            }
        }

        String requestId = "training-evaluation-" + trainingId;
        EvaluateTrainingResponse response = aiClient.evaluateTraining(new EvaluateTrainingRequest(
                requestId,
                trainingId,
                studentId,
                training.getTrainingTemplate().getId(),
                1,
                finalResult
        ));
        BigDecimal accuracy = response.accuracy().setScale(2, RoundingMode.HALF_UP);
        LocalDateTime finishedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        if (training.getStartedAt() == null) {
            training.start(finishedAt);
        }
        saveWordAttemptLogs(student, training, finalResult.path("wordAttempts"));
        training.complete(writeJson(finalResult), accuracy, finishedAt);
        activateNextTraining(training);
        studentFeatureProfileService.recalculate(student);
        if (training.getDailyCurriculum().getStatus() == DailyCurriculumStatus.COMPLETED) {
            personalizedCurriculumPlanner.createNextIfAbsent(student);
        }
        return accuracy;
    }

    private void activateNextTraining(TrainingEntity completedTraining) {
        Integer completedSequence = completedTraining.getSequenceNo();
        if (completedSequence == null) {
            return;
        }
        completedTraining.getDailyCurriculum().getTrainings().stream()
                .filter(training -> training.getSequenceNo() != null)
                .filter(training -> training.getSequenceNo() > completedSequence)
                .min(Comparator.comparingInt(TrainingEntity::getSequenceNo))
                .filter(TrainingEntity::isEditable)
                .filter(training -> trainingDataRepository.findByTrainingId(training.getId()).isPresent())
                .ifPresent(TrainingEntity::markReady);
    }

    @Transactional
    public void addExpectedWord(Long teacherId, Long studentId, Long trainingId, ExpectedWordRequest request) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        TrainingDataEntity data = findOrCreateTrainingData(training);
        ObjectNode root = parseObject(data.getGeneratedData());
        ArrayNode words = root.withArray("expectedWords");
        if (containsWordName(words, request.wordName())) {
            throw new IllegalArgumentException("이미 추가된 예정 단어입니다.");
        }
        WordEntity word = wordRepository.findByContent(request.wordName())
                .orElseGet(() -> wordRepository.save(new WordEntity(request.wordName())));
        ObjectNode wordNode = words.addObject();
        wordNode.put("wordId", word.getId());
        wordNode.put("wordName", word.getContent());
        data.updateGeneratedData(writeJson(root));
        training.markNotReady();
    }

    @Transactional
    public void deleteExpectedWord(Long teacherId, Long studentId, Long trainingId, Long wordId) {
        TrainingEntity training = findOwnedTraining(teacherId, studentId, trainingId);
        TrainingDataEntity data = trainingDataRepository.findByTrainingId(trainingId)
                .orElseThrow(() -> new ResourceNotFoundException("예정 단어를 찾을 수 없습니다."));
        ObjectNode root = parseObject(data.getGeneratedData());
        ArrayNode words = root.withArray("expectedWords");
        boolean removed = false;
        for (int index = words.size() - 1; index >= 0; index--) {
            if (words.get(index).path("wordId").asLong() == wordId) {
                words.remove(index);
                removed = true;
            }
        }
        if (!removed) throw new ResourceNotFoundException("예정 단어를 찾을 수 없습니다.");
        data.updateGeneratedData(writeJson(root));
        training.markNotReady();
    }

    private StudentEntity validateStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }

    private void saveWordAttemptLogs(StudentEntity student, TrainingEntity training, JsonNode attempts) {
        if (!attempts.isArray() || attempts.isEmpty()) {
            return;
        }

        Map<String, WordEntity> words = new HashMap<>();
        List<PendingAttemptLog> pendingLogs = new ArrayList<>();
        for (JsonNode attempt : attempts) {
            if (attempt.hasNonNull("wordAttemptLogId")) {
                continue;
            }
            String surfaceText = requiredSurfaceText(attempt);
            boolean hasAudioData = attempt.path("hasAudioData").asBoolean(false);
            boolean hasGazeData = attempt.path("hasGazeData").asBoolean(false);
            Boolean skipped = nullableBoolean(attempt, "isSkipped");
            Boolean correct = nullableBoolean(attempt, "isCorrect");
            Integer fixationDurationMs = nullableInteger(attempt, "fixationDurationMs");
            Integer fixationCount = nullableInteger(attempt, "fixationCount");
            Integer gazeStartOffsetMs = nullableInteger(attempt, "gazeStartOffsetMs");
            Integer gazeEndOffsetMs = nullableInteger(attempt, "gazeEndOffsetMs");
            Integer regressionCount = nullableInteger(attempt, "regressionCount");
            if (hasGazeData && fixationDurationMs == null
                    && fixationCount == null && regressionCount == null) {
                GazeWordAnalysisResult gaze = gazeWordAnalysisAdapter.analyze(
                        new GazeWordAnalysisRequest(
                                "training-gaze-" + training.getId() + "-" + pendingLogs.size(),
                                surfaceText,
                                gazeStartOffsetMs,
                                gazeEndOffsetMs
                        )
                );
                fixationDurationMs = gaze.fixationDurationMs();
                fixationCount = gaze.fixationCount();
                regressionCount = gaze.regressionCount();
                skipped = gaze.skipped();
                if (attempt instanceof ObjectNode objectAttempt) {
                    objectAttempt.put("fixationDurationMs", fixationDurationMs);
                    objectAttempt.put("fixationCount", fixationCount);
                    objectAttempt.put("regressionCount", regressionCount);
                    objectAttempt.put("isSkipped", skipped);
                    objectAttempt.put("gazeAnalysisConfidence", gaze.confidence());
                    objectAttempt.put("gazeAnalysisVersion", gaze.analysisVersion());
                }
            }
            Integer retryCount = nullableInteger(attempt, "retryCount");
            if (retryCount == null) {
                retryCount = regressionCount;
            }
            if (regressionCount == null) {
                regressionCount = retryCount;
            }
            Integer pronunciationAccuracyScore =
                    nullableInteger(attempt, "pronunciationAccuracyScore");
            if (pronunciationAccuracyScore == null
                    && attempt.hasNonNull("pronunciationScore")) {
                pronunciationAccuracyScore =
                        (int) Math.round(attempt.path("pronunciationScore").asDouble() * 10);
            }
            if (retryCount != null && retryCount < 0) {
                throw new IllegalArgumentException("단어 재응시 횟수는 0 이상이어야 합니다.");
            }
            Integer questionNo = nullableInteger(attempt, "questionNo");
            var requiredInputs = questionNo == null
                    ? java.util.Set.<TrainingInputType>of()
                    : trainingInputRequirementService.inputsForQuestion(
                            training.getId(),
                            questionNo
                    );
            boolean pronunciationRequired = requiredInputs.isEmpty()
                    ? hasAudioData
                    : requiredInputs.contains(TrainingInputType.VOICE);
            boolean gazeRequired = requiredInputs.isEmpty()
                    ? hasGazeData
                    : requiredInputs.contains(TrainingInputType.GAZE);
            Integer totalScore = wordAttemptScoreCalculator.calculate(
                    pronunciationAccuracyScore,
                    pronunciationRequired,
                    hasAudioData,
                    skipped,
                    gazeRequired,
                    hasGazeData,
                    skipped,
                    regressionCount,
                    retryCount,
                    correct
            );
            WordEntity word = words.computeIfAbsent(surfaceText, text ->
                    wordRepository.findByContent(text)
                            .orElseGet(() -> wordRepository.save(new WordEntity(text))));
            WordAttemptLogEntity log = new WordAttemptLogEntity(
                    student,
                    word,
                    training,
                    surfaceText,
                    hasAudioData,
                    fixationDurationMs,
                    fixationCount,
                    gazeStartOffsetMs,
                    gazeEndOffsetMs,
                    skipped,
                    regressionCount,
                    pronunciationAccuracyScore,
                    nullableInteger(attempt, "speechStartOffsetMs"),
                    nullableInteger(attempt, "speechEndOffsetMs"),
                    correct,
                    totalScore,
                    questionNo,
                    nullableInteger(attempt, "targetIndex"),
                    nullableInteger(attempt, "tokenIndex"),
                    attempt.path("isFinal").asBoolean(true)
            );
            if (attempt instanceof ObjectNode objectAttempt) {
                if (!objectAttempt.has("isFinal")) {
                    objectAttempt.put("isFinal", true);
                }
                pendingLogs.add(new PendingAttemptLog(objectAttempt, log));
            }
        }
        if (pendingLogs.isEmpty()) {
            return;
        }
        List<WordAttemptLogEntity> saved = wordAttemptLogRepository.saveAllAndFlush(
                pendingLogs.stream().map(PendingAttemptLog::log).toList()
        );
        for (int index = 0; index < saved.size(); index++) {
            pendingLogs.get(index).attempt().put("wordAttemptLogId", saved.get(index).getId());
        }
    }

    private record PendingAttemptLog(ObjectNode attempt, WordAttemptLogEntity log) {
    }

    private String requiredSurfaceText(JsonNode attempt) {
        String surfaceText = nullableText(attempt, "surfaceText", 50);
        if (surfaceText == null) {
            throw new IllegalArgumentException("단어 시도 로그의 surfaceText는 필수입니다.");
        }
        return surfaceText;
    }

    private String nullableText(JsonNode node, String field, int maxLength) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        String value = node.path(field).asText().trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + "의 길이는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return value;
    }

    private Integer nullableInteger(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asInt() : null;
    }

    private Boolean nullableBoolean(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asBoolean() : null;
    }

    private DailyCurriculumEntity findCurriculum(Long studentId, Long curriculumId) {
        return dailyCurriculumRepository.findByIdAndStudentId(curriculumId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "일일 커리큘럼을 찾을 수 없습니다."
                ));
    }

    private TrainingEntity findOwnedTraining(Long teacherId, Long studentId, Long trainingId) {
        validateStudentOwner(teacherId, studentId);
        return trainingRepository.findByIdAndDailyCurriculumStudentId(trainingId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("훈련을 찾을 수 없습니다."));
    }

    private List<TrainingTemplateEntity> resolveTemplates(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        Map<Long, TrainingTemplateEntity> templates = trainingTemplateRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TrainingTemplateEntity::getId, Function.identity()));
        if (templates.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("존재하지 않는 훈련 템플릿이 있습니다.");
        }
        return ids.stream().map(templates::get).toList();
    }

    private DailyCurriculumResponse toDailyCurriculumResponse(DailyCurriculumEntity curriculum) {
        return new DailyCurriculumResponse(
                curriculum.getId(),
                curriculum.getStatus().name(),
                curriculum.getTrainings().stream()
                .map(training -> new DailyCurriculumResponse.TrainingItem(
                        training.getId(),
                        training.getTrainingTemplate().getId(),
                        training.getSequenceNo(),
                        training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                        training.getTrainingTemplate().getName(),
                        training.getStatus().name()
                ))
                .toList()
        );
    }

    private BigDecimal averageAccuracy(DailyCurriculumEntity curriculum) {
        List<BigDecimal> values = curriculum.getTrainings().stream().map(TrainingEntity::getAccuracy)
                .filter(Objects::nonNull).toList();
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private CurriculumLogResponse.TrainingItem toCurriculumLogItem(TrainingEntity training) {
        return new CurriculumLogResponse.TrainingItem(training.getId(),
                training.getTrainingTemplate().getCurriculumUnit().getUnitName(),
                training.getTrainingTemplate().getName());
    }

    private TrainingEntity previousTraining(Long studentId, TrainingEntity current) {
        if (current.getFinishedAt() == null) return null;
        List<TrainingEntity> records = trainingRepository
                .findAllByDailyCurriculumStudentIdAndTrainingTemplateIdAndStatusAndFinishedAtLessThanOrderByFinishedAtDesc(
                        studentId, current.getTrainingTemplate().getId(), TrainingStatus.COMPLETED, current.getFinishedAt());
        return records.isEmpty() ? null : records.getFirst();
    }

    private LocalDate dateOf(TrainingEntity training) {
        return training.getFinishedAt() == null ? null : training.getFinishedAt().toLocalDate();
    }

    private List<TrainingLogResponse.QuestionResult> parseQuestionResults(String result) {
        if (result == null || result.isBlank()) return List.of();
        JsonNode questions = parseObject(result).path("questions");
        if (!questions.isArray()) return List.of();
        List<TrainingLogResponse.QuestionResult> response = new ArrayList<>();
        questions.forEach(q -> response.add(new TrainingLogResponse.QuestionResult(
                q.path("questionNumber").asInt(),
                q.path("isCorrect").asBoolean()
        )));
        return response;
    }

    private List<TrainingLogResponse.IncorrectItem> parseIncorrectItems(String result) {
        if (result == null || result.isBlank()) return List.of();
        JsonNode questions = parseObject(result).path("questions");
        if (!questions.isArray()) return List.of();
        List<TrainingLogResponse.IncorrectItem> response = new ArrayList<>();
        questions.forEach(question -> {
            if (question.path("isCorrect").asBoolean()) {
                return;
            }
            response.add(new TrainingLogResponse.IncorrectItem(
                    question.path("questionNumber").asInt(),
                    question.path("question").asText(null),
                    question.path("correctAnswer").asText(null),
                    question.path("selectedAnswer").asText(null)
            ));
        });
        return response;
    }

    private TrainingDataEntity findOrCreateTrainingData(TrainingEntity training) {
        return trainingDataRepository.findByTrainingId(training.getId())
                .orElseGet(() -> trainingDataRepository.save(new TrainingDataEntity(training,
                        "{\"version\":1,\"expectedWords\":[],\"content\":{}}")));
    }

    private ObjectNode trainingDataRoot(TrainingEntity training, boolean required) {
        Optional<TrainingDataEntity> data = trainingDataRepository.findByTrainingId(training.getId());
        if (data.isEmpty()) {
            if (required) {
                throw new ResourceNotFoundException("훈련 데이터를 찾을 수 없습니다.");
            }
            return null;
        }
        return parseObject(data.get().getGeneratedData());
    }

    private boolean containsWordName(ArrayNode words, String wordName) {
        for (JsonNode word : words) if (wordName.equals(word.path("wordName").asText())) return true;
        return false;
    }

    private ObjectNode parseObject(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node instanceof ObjectNode object) return object;
            throw new IllegalArgumentException("JSON 객체 형식이 필요합니다.");
        } catch (Exception exception) {
            throw new IllegalArgumentException("저장된 훈련 데이터 형식이 올바르지 않습니다.");
        }
    }

    private JsonNode parseNullableObject(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return parseObject(json);
    }

    private byte[] writeBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 결과 파일 생성에 실패했습니다.");
        }
    }

    private String toCsv(TrainingDetailResponse detail) {
        return String.join(",",
                "trainingId",
                "trainingTemplateId",
                "name",
                "status",
                "startedAt",
                "finishedAt",
                "accuracy",
                "result"
        ) + System.lineSeparator() + String.join(",",
                csv(detail.trainingId()),
                csv(detail.trainingTemplateId()),
                csv(detail.name()),
                csv(detail.status()),
                csv(detail.startedAt()),
                csv(detail.finishedAt()),
                csv(detail.accuracy()),
                csv(detail.result())
        ) + System.lineSeparator();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString()
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private ObjectNode validateGeneratedData(JsonNode generatedData) {
        if (!(generatedData instanceof ObjectNode root)) {
            throw new IllegalArgumentException("AI가 생성한 훈련 데이터는 JSON 객체여야 합니다.");
        }
        JsonNode questions = root.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new IllegalArgumentException("AI가 생성한 훈련 데이터에는 questions가 한 개 이상 필요합니다.");
        }
        Set<String> questionIds = new HashSet<>();
        for (JsonNode question : questions) {
            String questionId = question.path("questionId").asText();
            if (questionId.isBlank() || !questionIds.add(questionId)) {
                throw new IllegalArgumentException("각 문제에는 중복되지 않는 questionId가 필요합니다.");
            }
            if (!question.path("problem").isObject()) {
                throw new IllegalArgumentException("각 문제에는 problem JSON 객체가 필요합니다.");
            }
            if (!question.path("answer").isObject()) {
                throw new IllegalArgumentException("각 문제에는 answer JSON 객체가 필요합니다.");
            }
        }
        return root.deepCopy();
    }

    private String writeJson(JsonNode node) {
        try { return objectMapper.writeValueAsString(node); }
        catch (Exception exception) { throw new IllegalStateException("훈련 데이터 저장에 실패했습니다."); }
    }
}
