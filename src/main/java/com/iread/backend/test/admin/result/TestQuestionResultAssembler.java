package com.iread.backend.test.admin.result;

import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.test.admin.dto.res.TestCurriculumDetailResponse;
import com.iread.backend.test.domain.StudentTestEntity;
import com.iread.backend.test.domain.TestDataEntity;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.wordattempt.domain.WordAttemptLogEntity;
import com.iread.backend.wordattempt.repository.WordAttemptLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TestQuestionResultAssembler {
    private final TestDataRepository testDataRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final AppLearningQuestionSupport questionSupport;
    private final TestScoreNormalizer scoreNormalizer;
    private final TestTrackResolver trackResolver;
    private final ObjectMapper objectMapper;

    public TestCurriculumDetailResponse.QuestionResult assemble(StudentTestEntity test) {
        List<TestCurriculumDetailResponse.QuestionResult> results = assembleAll(test);
        if (results.size() != 1) {
            throw new IllegalStateException(
                    "실력도전 개별 검사에는 출제 문항이 정확히 1개여야 합니다: " + test.getId()
            );
        }
        return results.getFirst();
    }

    public List<TestCurriculumDetailResponse.QuestionResult> assembleAll(StudentTestEntity test) {
        JsonNode questions = readQuestions(test.getId());
        JsonNode result = readObject(test.getResult(), "저장된 검사 결과");
        return java.util.stream.IntStream.range(0, questions.size())
                .mapToObj(index -> assemble(
                        test,
                        questions.get(index),
                        result,
                        index,
                        questions.size()
                ))
                .toList();
    }

    private TestCurriculumDetailResponse.QuestionResult assemble(
            StudentTestEntity test,
            JsonNode question,
            JsonNode result,
            int questionIndex,
            int questionCount
    ) {
        int questionNo = question.path("questionNo").asInt(1);
        JsonNode submission = findSubmission(result.path("submissions"), questionNo);
        JsonNode legacyResult = findLegacyQuestionResult(
                result.path("questions"), questionNo, questionIndex
        );
        LearningResponseType responseType = questionSupport.responseType(question);
        List<WordAttemptLogEntity> finalAttempts = responseType == LearningResponseType.AUDIO
                ? wordAttemptLogRepository
                .findAllByTestIdAndQuestionNoAndFinalAttemptTrue(test.getId(), questionNo)
                : List.of();
        JsonNode latestAnalysis = latestPronunciationAnalysis(
                result.path("pronunciationAnalyses"),
                questionNo
        );
        int sequenceNo = questionCount == 1
                ? test.getSequenceNo()
                : (test.getSequenceNo() - 1) * questionCount + questionIndex + 1;
        TestTrackResolver.Track track = trackResolver.resolve(sequenceNo);

        return new TestCurriculumDetailResponse.QuestionResult(
                test.getId(),
                sequenceNo,
                track.code(),
                questionType(question),
                questionText(question, legacyResult, test.getTrainingTemplate().getName()),
                responseType.name(),
                selectedAnswer(responseType, question, submission, legacyResult),
                correctAnswer(responseType, question, legacyResult),
                correct(responseType, submission, latestAnalysis, finalAttempts, legacyResult),
                score(responseType, submission, finalAttempts, test.getAccuracy()),
                pronunciationScore(latestAnalysis, finalAttempts),
                questionIndex == 0 ? nullableLong(result.get("solvingTimeSeconds")) : null,
                questionIndex == 0 ? nullableInteger(result.get("gazeDepartureCount")) : null
        );
    }

    private JsonNode readQuestions(Long testId) {
        String stored = testDataRepository
                .findFirstByTestIdOrderByCreatedAtDescIdDesc(testId)
                .map(TestDataEntity::getGeneratedData)
                .orElseThrow(() -> new IllegalStateException(
                        "검사 출제 데이터가 없습니다: " + testId
                ));
        JsonNode questions = readObject(stored, "저장된 검사 출제 데이터").path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new IllegalStateException(
                    "실력도전 검사에는 출제 문항이 1개 이상이어야 합니다: " + testId
            );
        }
        return questions;
    }
    private JsonNode readObject(String stored, String label) {
        if (stored == null || stored.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode value = objectMapper.readTree(stored);
            if (value != null && value.isObject()) {
                return value;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalStateException(label + " 형식이 올바르지 않습니다.");
    }

    private JsonNode findSubmission(JsonNode submissions, int questionNo) {
        if (!submissions.isArray()) {
            return NullNode.getInstance();
        }
        for (JsonNode submission : submissions) {
            if (submission.path("questionNo").asInt() == questionNo) {
                return submission;
            }
        }
        return NullNode.getInstance();
    }

    private JsonNode findLegacyQuestionResult(
            JsonNode results,
            int questionNo,
            int questionIndex
    ) {
        if (!results.isArray()) {
            return NullNode.getInstance();
        }
        for (JsonNode result : results) {
            int storedQuestionNo = result.path("questionNo").asInt(
                    result.path("questionNumber").asInt(-1)
            );
            if (storedQuestionNo == questionNo) {
                return result;
            }
        }
        return questionIndex < results.size()
                ? results.get(questionIndex)
                : NullNode.getInstance();
    }

    private JsonNode latestPronunciationAnalysis(JsonNode analyses, int questionNo) {
        JsonNode latest = NullNode.getInstance();
        int latestAttempt = Integer.MIN_VALUE;
        if (!analyses.isArray()) {
            return latest;
        }
        for (JsonNode analysis : analyses) {
            if (analysis.path("questionNo").asInt() != questionNo) {
                continue;
            }
            int attempt = analysis.path("attemptNo").asInt(0);
            if (attempt >= latestAttempt) {
                latest = analysis;
                latestAttempt = attempt;
            }
        }
        return latest;
    }

    private JsonNode selectedAnswer(
            LearningResponseType type,
            JsonNode question,
            JsonNode submission,
            JsonNode legacyResult
    ) {
        if (legacyResult.hasNonNull("selectedAnswer")) {
            return legacyResult.path("selectedAnswer").deepCopy();
        }
        if (type == LearningResponseType.AUDIO) {
            return NullNode.getInstance();
        }
        JsonNode response = submission.path("response");
        if (!response.isObject()) {
            return NullNode.getInstance();
        }
        return switch (type) {
            case SINGLE_CHOICE -> choiceValue(
                    question.path("content").path("choices"),
                    response.path("selectedIndex").asInt(-1)
            );
            case ORDERING -> copyOrNull(response.get("orderedIndexes"));
            case COMPONENT_BUILD -> copyOrNull(response.get("selections"));
            case TEXT_INPUT -> copyOrNull(response.get("text"));
            case TRACE -> response.deepCopy();
            case AUDIO -> NullNode.getInstance();
        };
    }

    private JsonNode correctAnswer(
            LearningResponseType type,
            JsonNode question,
            JsonNode legacyResult
    ) {
        if (legacyResult.hasNonNull("correctAnswer")) {
            return legacyResult.path("correctAnswer").deepCopy();
        }
        JsonNode answer = question.path("answer").isObject()
                ? question.path("answer")
                : question.path("content");
        return switch (type) {
            case SINGLE_CHOICE -> choiceValue(
                    question.path("content").path("choices"),
                    "SYLLABLE_DELETE".equals(questionType(question))
                            ? answer.path("deleteIndex").asInt(-1)
                            : answer.path("answerIndex").asInt(-1)
            );
            case ORDERING -> copyOrNull(answer.get("answerOrder"));
            case COMPONENT_BUILD -> componentAnswer(answer);
            case TEXT_INPUT -> answer.path("acceptedAnswers").isArray()
                    && !answer.path("acceptedAnswers").isEmpty()
                    ? answer.path("acceptedAnswers").get(0).deepCopy()
                    : NullNode.getInstance();
            case TRACE -> firstText(answer, "target", "result");
            case AUDIO -> expectedAudioText(question, answer);
        };
    }

    private JsonNode componentAnswer(JsonNode answer) {
        ObjectNode result = objectMapper.createObjectNode();
        putIfNumber(result, "INITIAL", answer.get("initialAnswerIndex"));
        putIfNumber(result, "MEDIAL", answer.get("medialAnswerIndex"));
        putIfNumber(result, "FINAL", answer.get("finalAnswerIndex"));
        return result.isEmpty() ? NullNode.getInstance() : result;
    }

    private void putIfNumber(ObjectNode target, String field, JsonNode value) {
        if (value != null && value.isIntegralNumber()) {
            target.put(field, value.asInt());
        }
    }

    private JsonNode expectedAudioText(JsonNode question, JsonNode answer) {
        JsonNode direct = firstText(
                answer,
                "expectedText",
                "result",
                "completedSentence",
                "target"
        );
        if (!direct.isNull()) {
            return direct;
        }
        JsonNode targets = question.path("analysisTargets");
        if (targets.isArray() && !targets.isEmpty()) {
            JsonNode text = copyOrNull(targets.get(0).get("text"));
            if (!text.isNull()) {
                return text;
            }
        }
        return copyOrNull(question.get("text"));
    }

    private JsonNode firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.deepCopy();
            }
        }
        return NullNode.getInstance();
    }

    private JsonNode choiceValue(JsonNode choices, int index) {
        if (!choices.isArray() || index < 0 || index >= choices.size()) {
            return NullNode.getInstance();
        }
        JsonNode choice = choices.get(index);
        if (choice.isObject() && choice.path("text").isTextual()) {
            return choice.path("text").deepCopy();
        }
        return choice.deepCopy();
    }

    private Boolean correct(
            LearningResponseType type,
            JsonNode submission,
            JsonNode latestAnalysis,
            List<WordAttemptLogEntity> attempts,
            JsonNode legacyResult
    ) {
        if (legacyResult.hasNonNull("isCorrect")) {
            return legacyResult.path("isCorrect").asBoolean();
        }
        if (type != LearningResponseType.AUDIO) {
            return submission.hasNonNull("correct")
                    ? submission.path("correct").asBoolean()
                    : null;
        }
        if (latestAnalysis.hasNonNull("passed")) {
            return latestAnalysis.path("passed").asBoolean();
        }
        if (attempts.isEmpty() || attempts.stream().anyMatch(
                attempt -> attempt.getCorrect() == null
        )) {
            return null;
        }
        return attempts.stream().allMatch(
                attempt -> Boolean.TRUE.equals(attempt.getCorrect())
        );
    }

    private BigDecimal score(
            LearningResponseType type,
            JsonNode submission,
            List<WordAttemptLogEntity> attempts,
            BigDecimal storedAccuracy
    ) {
        BigDecimal score = type == LearningResponseType.AUDIO
                ? scoreNormalizer.average(attempts.stream()
                .map(WordAttemptLogEntity::getTotalScore)
                .map(scoreNormalizer::fromStoredTotal)
                .toList())
                : scoreNormalizer.fromStoredTotal(submission.get("totalScore"));
        if (score != null) {
            return score;
        }
        return storedAccuracy == null
                ? null
                : storedAccuracy.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal pronunciationScore(
            JsonNode latestAnalysis,
            List<WordAttemptLogEntity> attempts
    ) {
        BigDecimal analysisScore = scoreNormalizer.fromPronunciation(
                latestAnalysis.get("pronunciationAccuracyScore")
        );
        if (analysisScore != null) {
            return analysisScore;
        }
        return scoreNormalizer.average(attempts.stream()
                .map(WordAttemptLogEntity::getPronunciationAccuracyScore)
                .map(value -> value == null
                        ? null
                        : scoreNormalizer.fromPronunciation(
                        objectMapper.getNodeFactory().numberNode(value)
                ))
                .toList());
    }

    private String questionType(JsonNode question) {
        String type = question.path("type").asText(
                question.path("questionType").asText()
        );
        return type.isBlank() && question.hasNonNull("text")
                ? "SENTENCE_READING"
                : type;
    }

    private String questionText(
            JsonNode question,
            JsonNode legacyResult,
            String templateName
    ) {
        for (JsonNode candidate : List.of(
                question.path("text"),
                question.path("question"),
                question.path("content").path("instruction"),
                question.path("content").path("prompt")
        )) {
            if (candidate.isTextual() && !candidate.asText().isBlank()) {
                return candidate.asText();
            }
        }
        if (legacyResult.path("question").isTextual()
                && !legacyResult.path("question").asText().isBlank()) {
            return legacyResult.path("question").asText();
        }
        return templateName;
    }

    private JsonNode copyOrNull(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode()
                ? NullNode.getInstance()
                : node.deepCopy();
    }

    private Long nullableLong(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber()
                ? null
                : node.asLong();
    }

    private Integer nullableInteger(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber()
                ? null
                : node.asInt();
    }
}
