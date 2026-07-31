package com.iread.backend.gaze.analysis;

import com.iread.backend.exception.ConflictException;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputRequirementService;
import com.iread.backend.training.input.TrainingInputType;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import com.iread.backend.wordattempt.service.WordAttemptScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GazeWordMetricMergeService {

    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final TrainingInputRequirementService trainingInputRequirementService;
    private final TestDataRepository testDataRepository;
    private final WordAttemptScoreCalculator scoreCalculator;
    private final ObjectMapper objectMapper;

    public void merge(GazeSessionEntity session, JsonNode data) {
        GazeContentType contentType = session.getContentType();
        if (contentType != GazeContentType.TRAINING
                && contentType != GazeContentType.TEST
                && contentType != GazeContentType.STORY) {
            return;
        }
        JsonNode words = data == null || !data.isObject()
                ? objectMapper.missingNode()
                : data.path("words");
        if (!words.isArray() || words.isEmpty()) {
            if (contentType == GazeContentType.STORY) {
                // 스토리는 문장 단위 지표만 보내는 경우도 허용한다.
                return;
            }
            throw new IllegalArgumentException(
                    "검사·훈련 시선 세션에는 단어별 words 지표가 필요합니다."
            );
        }
        boolean story = contentType == GazeContentType.STORY;
        List<WordMetric> metrics = new ArrayList<>();
        Set<WordPosition> positions = new HashSet<>();
        words.forEach(word -> {
            WordMetric metric = story ? readStoryMetric(word) : readMetric(word);
            WordPosition position = new WordPosition(
                    metric.questionNo(),
                    metric.targetIndex(),
                    metric.tokenIndex(),
                    metric.storyLineId()
            );
            if (!positions.add(position)) {
                throw new IllegalArgumentException(
                        story
                                ? "같은 대사·토큰 위치의 시선 단어 지표는 중복할 수 없습니다."
                                : "같은 문항·대상·토큰 위치의 시선 단어 지표는 중복할 수 없습니다."
                );
            }
            metrics.add(metric);
        });
        for (WordMetric metric : metrics) {
            mergeMetric(session, metric);
        }
    }

    private void mergeMetric(GazeSessionEntity session, WordMetric metric) {
        List<WordAttemptLogEntity> attempts;
        List<WordAttemptLogEntity> attemptHistory;
        boolean pronunciationRequired;
        if (session.getContentType() == GazeContentType.STORY) {
            long storyId = session.getStory().getId();
            attemptHistory = wordAttemptLogRepository
                    .findAllByStoryLineId(metric.storyLineId())
                    .stream()
                    .filter(attempt -> java.util.Objects.equals(
                            attempt.getStoryLine().getStory().getId(),
                            storyId
                    ))
                    .filter(attempt -> sameTokenPosition(
                            metric.tokenIndex(),
                            attempt.getTokenIndex()
                    ))
                    .filter(attempt -> sameText(
                            metric.text(),
                            attempt.getSurfaceText()
                    ))
                    .toList();
            attempts = attemptHistory.stream()
                    .filter(WordAttemptLogEntity::isFinalAttempt)
                    .toList();
            // 스토리 대사는 항상 소리 내어 읽으므로 발음 점수가 종합 점수에 들어간다.
            pronunciationRequired = true;
        } else if (session.getContentType() == GazeContentType.TRAINING) {
            long trainingId = session.getTraining().getId();
            Set<TrainingInputType> inputs =
                    trainingInputRequirementService.inputsForQuestion(
                            trainingId,
                            metric.questionNo()
                    );
            if (!inputs.contains(TrainingInputType.GAZE)) {
                throw new ConflictException(
                        "시선 입력을 사용하지 않는 훈련 문항의 단어 지표입니다."
                );
            }
            pronunciationRequired = inputs.contains(TrainingInputType.VOICE);
            attemptHistory = wordAttemptLogRepository
                    .findAllByTrainingIdAndQuestionNoAndTargetIndex(
                            trainingId,
                            metric.questionNo(),
                            metric.targetIndex()
                    )
                    .stream()
                    .filter(attempt -> sameTokenPosition(
                            metric.tokenIndex(),
                            attempt.getTokenIndex()
                    ))
                    .filter(attempt -> sameText(
                            metric.text(),
                            attempt.getSurfaceText()
                    ))
                    .toList();
            attempts = attemptHistory.stream()
                    .filter(WordAttemptLogEntity::isFinalAttempt)
                    .toList();
        } else {
            long testId = session.getTest().getId();
            Set<TrainingInputType> inputs = testInputs(
                    testId,
                    metric.questionNo()
            );
            if (!inputs.contains(TrainingInputType.GAZE)) {
                throw new ConflictException(
                        "시선 입력을 사용하지 않는 검사 문항의 단어 지표입니다."
                );
            }
            pronunciationRequired = inputs.contains(TrainingInputType.VOICE);
            attemptHistory = wordAttemptLogRepository
                    .findAllByTestIdAndQuestionNo(
                            testId,
                            metric.questionNo()
                    )
                    .stream()
                    .filter(attempt -> java.util.Objects.equals(
                            attempt.getTargetIndex(),
                            metric.targetIndex()
                    ))
                    .filter(attempt -> sameTokenPosition(
                            metric.tokenIndex(),
                            attempt.getTokenIndex()
                    ))
                    .filter(attempt -> sameText(
                            metric.text(),
                            attempt.getSurfaceText()
                    ))
                    .toList();
            attempts = attemptHistory.stream()
                    .filter(WordAttemptLogEntity::isFinalAttempt)
                    .toList();
        }

        if (attempts.size() != 1) {
            throw new ConflictException(
                    "단어별 시선 지표를 최종 단어 시도와 하나로 연결할 수 없습니다."
            );
        }
        WordAttemptLogEntity attempt = attempts.getFirst();
        boolean gazeSkipped = resolveGazeSkipped(metric);
        // 스토리는 다시 읽어도 재시도로 감점하지 않는다.
        int retryCount = session.getContentType() == GazeContentType.STORY
                ? 0
                : Math.max(0, attemptHistory.size() - 1);
        Integer totalScore = scoreCalculator.calculate(
                attempt.getPronunciationAccuracyScore(),
                pronunciationRequired,
                attempt.isHasAudioData(),
                attempt.getSkipped(),
                true,
                true,
                gazeSkipped,
                metric.regressionCount(),
                retryCount,
                attempt.getCorrect()
        );
        attempt.applyGazeMetrics(
                metric.dwellMs(),
                metric.visitCount(),
                metric.firstSeenMs(),
                metric.lastSeenMs(),
                gazeSkipped,
                metric.regressionCount(),
                totalScore
        );
    }

    private Set<TrainingInputType> testInputs(long testId, int questionNo) {
        JsonNode generated = testDataRepository
                .findFirstByTestIdOrderByCreatedAtDescIdDesc(testId)
                .map(TestDataEntity::getGeneratedData)
                .map(this::readJson)
                .orElseThrow(() -> new ConflictException(
                        "검사 문항을 찾을 수 없습니다."
                ));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray()
                || questionNo < 1
                || questionNo > questions.size()) {
            throw new ConflictException("검사 문항을 찾을 수 없습니다.");
        }
        return TrainingInputPolicy.forQuestion(questions.get(questionNo - 1));
    }

    private WordMetric readMetric(JsonNode word) {
        int questionNo = requiredPositive(word, "questionNo");
        int targetIndex = requiredNonNegative(word, "targetIndex");
        int tokenIndex = word.hasNonNull("tokenIndex")
                ? requiredNonNegative(word, "tokenIndex")
                : requiredNonNegative(word, "index");
        String text = word.path("text").asText();
        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "시선 단어 지표의 text는 필수입니다."
            );
        }
        int dwellMs = requiredNonNegative(word, "dwellMs");
        int visitCount = requiredNonNegative(word, "visitCount");
        Integer readCount = nullableNonNegative(word, "readCount");
        Boolean skipped = nullableBoolean(word, "skipped");
        int regressionCount = requiredNonNegative(word, "regressionCount");
        Integer firstSeenMs = nullableNonNegative(word, "firstSeenMs");
        Integer lastSeenMs = nullableNonNegative(word, "lastSeenMs");
        if (firstSeenMs != null && lastSeenMs != null
                && lastSeenMs < firstSeenMs) {
            throw new IllegalArgumentException(
                    "lastSeenMs는 firstSeenMs보다 빠를 수 없습니다."
            );
        }
        return new WordMetric(
                questionNo,
                targetIndex,
                tokenIndex,
                null,
                text,
                dwellMs,
                visitCount,
                readCount,
                skipped,
                regressionCount,
                firstSeenMs,
                lastSeenMs
        );
    }

    /**
     * 스토리 단어 지표. 스토리는 문항·대상 위치가 없어 대사와 토큰 위치가 키가 된다.
     */
    private WordMetric readStoryMetric(JsonNode word) {
        if (!word.hasNonNull("storyLineId") || !word.path("storyLineId").canConvertToLong()) {
            throw new IllegalArgumentException("storyLineId는 필수입니다.");
        }
        long storyLineId = word.path("storyLineId").asLong();
        if (storyLineId < 1) {
            throw new IllegalArgumentException("storyLineId는 1 이상이어야 합니다.");
        }
        int tokenIndex = word.hasNonNull("tokenIndex")
                ? requiredNonNegative(word, "tokenIndex")
                : requiredNonNegative(word, "index");
        String text = word.path("text").asText();
        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "시선 단어 지표의 text는 필수입니다."
            );
        }
        int dwellMs = requiredNonNegative(word, "dwellMs");
        int visitCount = requiredNonNegative(word, "visitCount");
        Integer readCount = nullableNonNegative(word, "readCount");
        Boolean skipped = nullableBoolean(word, "skipped");
        int regressionCount = requiredNonNegative(word, "regressionCount");
        Integer firstSeenMs = nullableNonNegative(word, "firstSeenMs");
        Integer lastSeenMs = nullableNonNegative(word, "lastSeenMs");
        if (firstSeenMs != null && lastSeenMs != null
                && lastSeenMs < firstSeenMs) {
            throw new IllegalArgumentException(
                    "lastSeenMs는 firstSeenMs보다 빠를 수 없습니다."
            );
        }
        return new WordMetric(
                0,
                null,
                tokenIndex,
                storyLineId,
                text,
                dwellMs,
                visitCount,
                readCount,
                skipped,
                regressionCount,
                firstSeenMs,
                lastSeenMs
        );
    }

    private boolean resolveGazeSkipped(WordMetric metric) {
        if (metric.skipped() != null) {
            return metric.skipped();
        }
        if (metric.readCount() != null) {
            return metric.readCount() == 0;
        }
        return metric.visitCount() == 0;
    }

    private int requiredPositive(JsonNode node, String field) {
        int value = requiredNonNegative(node, field);
        if (value == 0) {
            throw new IllegalArgumentException(field + "는 1 이상이어야 합니다.");
        }
        return value;
    }

    private int requiredNonNegative(JsonNode node, String field) {
        if (!node.hasNonNull(field)
                || !node.path(field).isIntegralNumber()
                || !node.path(field).canConvertToInt()) {
            throw new IllegalArgumentException(field + "는 필수 정수입니다.");
        }
        int value = node.path(field).asInt();
        if (value < 0) {
            throw new IllegalArgumentException(field + "는 0 이상이어야 합니다.");
        }
        return value;
    }

    private Integer nullableNonNegative(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return requiredNonNegative(node, field);
    }

    private Boolean nullableBoolean(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        if (!node.path(field).isBoolean()) {
            throw new IllegalArgumentException(field + " must be a boolean.");
        }
        return node.path(field).asBoolean();
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "저장된 검사 문항을 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private boolean sameText(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private boolean sameTokenPosition(Integer metricTokenIndex, Integer attemptTokenIndex) {
        return java.util.Objects.equals(metricTokenIndex, attemptTokenIndex)
                || (metricTokenIndex != null
                && metricTokenIndex == 0
                && attemptTokenIndex == null);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}ㄱ-ㅎㅏ-ㅣ]", "");
    }

    private record WordMetric(
            int questionNo,
            Integer targetIndex,
            Integer tokenIndex,
            Long storyLineId,
            String text,
            int dwellMs,
            int visitCount,
            Integer readCount,
            Boolean skipped,
            int regressionCount,
            Integer firstSeenMs,
            Integer lastSeenMs
    ) {
    }

    private record WordPosition(
            int questionNo,
            int targetIndex,
            int tokenIndex,
            Long storyLineId
    ) {
    }
}
