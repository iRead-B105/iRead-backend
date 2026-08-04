package com.iread.backend.training.admin.result;

import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.service.AppLearningQuestionSupport;
import com.iread.backend.training.admin.dto.res.TrainingLogResponse;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainingQuestionResultAssembler {
    private static final BigDecimal STORED_SCORE_SCALE = BigDecimal.TEN;
    private static final Set<String> ANSWER_FIELDS = Set.of(
            "answerIndex",
            "answerOrder",
            "initialAnswerIndex",
            "medialAnswerIndex",
            "finalAnswerIndex",
            "deleteIndex",
            "acceptedAnswers",
            "result",
            "completedSentence"
    );

    private final TrainingDataRepository trainingDataRepository;
    private final WordAttemptLogRepository wordAttemptLogRepository;
    private final AppLearningQuestionSupport questionSupport;
    private final ObjectMapper objectMapper;

    public List<TrainingLogResponse.TrainingQuestionResult> assembleAll(
            TrainingEntity training
    ) {
        JsonNode generated = trainingDataRepository.findByTrainingId(training.getId())
                .map(TrainingDataEntity::getGeneratedData)
                .map(value -> readObject(value, "저장된 훈련 출제 데이터"))
                .orElseGet(objectMapper::createObjectNode);
        JsonNode result = readObject(training.getResult(), "저장된 훈련 결과");
        List<JsonNode> sourceQuestions = sourceQuestions(generated, result);

        List<TrainingLogResponse.TrainingQuestionResult> assembled = new ArrayList<>();
        for (int index = 0; index < sourceQuestions.size(); index++) {
            JsonNode question = sourceQuestions.get(index);
            int questionNo = questionNo(question, index);
            JsonNode submission = findByQuestionNo(result.path("submissions"), questionNo);
            JsonNode storedResult = findStoredResult(
                    result.path("questions"),
                    questionNo,
                    index
            );
            LearningResponseType responseType = responseType(question, submission, storedResult);
            List<WordAttemptLogEntity> finalAttempts = responseType == LearningResponseType.AUDIO
                    ? wordAttemptLogRepository
                    .findAllByTrainingIdAndQuestionNoAndFinalAttemptTrue(
                            training.getId(),
                            questionNo
                    )
                    : List.of();
            JsonNode analysis = latestPronunciationAnalysis(
                    result.path("pronunciationAnalyses"),
                    questionNo
            );

            assembled.add(new TrainingLogResponse.TrainingQuestionResult(
                    questionNo,
                    questionType(question, storedResult),
                    questionValue(question, storedResult),
                    responseType == null ? null : responseType.name(),
                    selectedAnswer(responseType, question, submission, storedResult),
                    correctAnswer(responseType, question, storedResult),
                    correct(responseType, submission, storedResult, analysis, finalAttempts),
                    score(responseType, submission, storedResult, finalAttempts)
            ));
        }
        return List.copyOf(assembled);
    }

    private List<JsonNode> sourceQuestions(JsonNode generated, JsonNode result) {
        List<JsonNode> questions = new ArrayList<>();
        Set<Integer> questionNumbers = new HashSet<>();
        appendMissingQuestions(questions, questionNumbers, generated.path("questions"));
        appendMissingQuestions(questions, questionNumbers, result.path("questions"));
        appendMissingQuestions(questions, questionNumbers, result.path("submissions"));
        return List.copyOf(questions);
    }

    private void appendMissingQuestions(
            List<JsonNode> target,
            Set<Integer> questionNumbers,
            JsonNode candidates
    ) {
        if (!candidates.isArray()) {
            return;
        }
        for (JsonNode candidate : candidates) {
            int questionNo = questionNo(candidate, target.size());
            if (questionNumbers.add(questionNo)) {
                target.add(candidate);
            }
        }
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

    private int questionNo(JsonNode question, int index) {
        if (question.path("questionNo").isIntegralNumber()) {
            return question.path("questionNo").asInt();
        }
        if (question.path("questionNumber").isIntegralNumber()) {
            return question.path("questionNumber").asInt();
        }
        return index + 1;
    }

    private JsonNode findByQuestionNo(JsonNode values, int questionNo) {
        if (!values.isArray()) {
            return NullNode.getInstance();
        }
        for (JsonNode value : values) {
            if (value.path("questionNo").asInt(-1) == questionNo
                    || value.path("questionNumber").asInt(-1) == questionNo) {
                return value;
            }
        }
        return NullNode.getInstance();
    }

    private JsonNode findStoredResult(JsonNode values, int questionNo, int index) {
        JsonNode matched = findByQuestionNo(values, questionNo);
        if (!matched.isNull()) {
            return matched;
        }
        return values.isArray() && index < values.size()
                ? values.get(index)
                : NullNode.getInstance();
    }

    private LearningResponseType responseType(
            JsonNode question,
            JsonNode submission,
            JsonNode storedResult
    ) {
        for (JsonNode candidate : List.of(submission, storedResult, question)) {
            String stored = candidate.path("responseType").asText();
            if (!stored.isBlank()) {
                try {
                    return LearningResponseType.valueOf(stored);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        String questionType = questionType(question, storedResult);
        if (questionType != null) {
            try {
                return questionSupport.responseType(question);
            } catch (IllegalStateException ignored) {
            }
        }
        return null;
    }

    private String questionType(JsonNode question, JsonNode storedResult) {
        for (JsonNode candidate : List.of(question, storedResult)) {
            String type = candidate.path("type").asText(
                    candidate.path("questionType").asText()
            );
            if (!type.isBlank()) {
                return type;
            }
            if (candidate.hasNonNull("text")) {
                return "SENTENCE_READING";
            }
        }
        return null;
    }

    private JsonNode questionValue(JsonNode question, JsonNode storedResult) {
        JsonNode content = question.get("content");
        if (content != null && !content.isNull() && !content.isMissingNode()) {
            JsonNode value = content.deepCopy();
            if (value instanceof ObjectNode object) {
                ANSWER_FIELDS.forEach(object::remove);
            }
            return value;
        }
        for (JsonNode candidate : List.of(question, storedResult)) {
            JsonNode storedQuestion = candidate.get("question");
            if (storedQuestion != null && !storedQuestion.isNull()) {
                return normalizeQuestion(storedQuestion);
            }
            JsonNode text = candidate.get("text");
            if (text != null && text.isTextual() && !text.asText().isBlank()) {
                ObjectNode value = objectMapper.createObjectNode();
                value.set("text", text.deepCopy());
                return value;
            }
        }
        return NullNode.getInstance();
    }

    private JsonNode normalizeQuestion(JsonNode value) {
        if (!value.isTextual()) {
            return value.deepCopy();
        }
        ObjectNode question = objectMapper.createObjectNode();
        question.set("text", value.deepCopy());
        return question;
    }

    private JsonNode selectedAnswer(
            LearningResponseType type,
            JsonNode question,
            JsonNode submission,
            JsonNode storedResult
    ) {
        if (storedResult.hasNonNull("selectedAnswer")) {
            return storedResult.path("selectedAnswer").deepCopy();
        }
        if (type == LearningResponseType.AUDIO) {
            return NullNode.getInstance();
        }
        JsonNode response = submission.path("response");
        if (!response.isObject()) {
            return NullNode.getInstance();
        }
        if (type == null) {
            return response.deepCopy();
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
            JsonNode storedResult
    ) {
        JsonNode answer = question.path("answer").isObject()
                ? question.path("answer")
                : question.path("content");
        JsonNode derived = type == null
                ? copyOrNull(question.get("answer"))
                : switch (type) {
                    case SINGLE_CHOICE -> choiceValue(
                            question.path("content").path("choices"),
                            "SYLLABLE_DELETE".equals(questionType(question, storedResult))
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
        if (!derived.isNull()) {
            return derived;
        }
        return copyOrNull(storedResult.get("correctAnswer"));
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
            JsonNode storedResult,
            JsonNode analysis,
            List<WordAttemptLogEntity> attempts
    ) {
        for (String field : List.of("isCorrect", "correct")) {
            if (storedResult.hasNonNull(field)) {
                return storedResult.path(field).asBoolean();
            }
        }
        if (submission.hasNonNull("correct")) {
            return submission.path("correct").asBoolean();
        }
        if (type != LearningResponseType.AUDIO) {
            return null;
        }
        if (analysis.hasNonNull("passed")) {
            return analysis.path("passed").asBoolean();
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
            JsonNode storedResult,
            List<WordAttemptLogEntity> attempts
    ) {
        if (storedResult.path("score").isNumber()) {
            return storedResult.path("score").decimalValue().setScale(2, RoundingMode.HALF_UP);
        }
        if (type == LearningResponseType.AUDIO) {
            return average(attempts.stream()
                    .map(WordAttemptLogEntity::getTotalScore)
                    .map(this::fromStoredTotal)
                    .toList());
        }
        BigDecimal submissionScore = fromStoredTotal(submission.get("totalScore"));
        return submissionScore != null
                ? submissionScore
                : fromStoredTotal(storedResult.get("totalScore"));
    }

    private BigDecimal fromStoredTotal(JsonNode score) {
        return score == null || score.isNull() || !score.isNumber()
                ? null
                : score.decimalValue().divide(
                        STORED_SCORE_SCALE,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal fromStoredTotal(Integer score) {
        return score == null
                ? null
                : BigDecimal.valueOf(score).divide(
                        STORED_SCORE_SCALE,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal average(List<BigDecimal> scores) {
        List<BigDecimal> values = scores.stream().filter(value -> value != null).toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private JsonNode latestPronunciationAnalysis(JsonNode analyses, int questionNo) {
        JsonNode latest = NullNode.getInstance();
        int latestAttempt = Integer.MIN_VALUE;
        if (!analyses.isArray()) {
            return latest;
        }
        for (JsonNode analysis : analyses) {
            if (analysis.path("questionNo").asInt(-1) != questionNo) {
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

    private JsonNode copyOrNull(JsonNode value) {
        return value == null || value.isNull() || value.isMissingNode()
                ? NullNode.getInstance()
                : value.deepCopy();
    }
}
