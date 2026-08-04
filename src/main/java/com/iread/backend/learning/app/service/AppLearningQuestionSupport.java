package com.iread.backend.learning.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.learning.app.dto.LearningErrorLocation;
import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.dto.LearningSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class AppLearningQuestionSupport {

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

    private final ObjectMapper objectMapper;
    private final AiClient aiClient;

    @Autowired
    public AppLearningQuestionSupport(ObjectMapper objectMapper, AiClient aiClient) {
        this.objectMapper = objectMapper;
        this.aiClient = aiClient;
    }

    public AppLearningQuestionSupport(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public ObjectNode toStudentQuestion(JsonNode question) {
        String questionType = questionType(question);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("questionType", questionType);
        result.put("responseType", responseType(question).name());
        ObjectNode content = studentContent(question);
        enrichImageUrls(content);
        result.set("content", content);
        result.set("answer", answer(question).deepCopy());
        if (question.has("requiredInputs")) {
            result.set("requiredInputs", question.path("requiredInputs").deepCopy());
        }
        addRecordingTargets(result, question);
        return result;
    }

    public Evaluation evaluate(JsonNode question, LearningSubmission submission) {
        LearningResponseType expectedType = responseType(question);
        if (expectedType == LearningResponseType.AUDIO) {
            throw new IllegalArgumentException("음성 문항은 recordings API로 제출해야 합니다.");
        }
        if (submission.responseType() != expectedType) {
            throw new IllegalArgumentException("문항의 responseType과 제출 responseType이 일치하지 않습니다.");
        }
        validateResponse(submission);
        JsonNode answer = answer(question);
        return switch (expectedType) {
            case TRACE -> new Evaluation(true, 1000, List.of(), correctTraceResponse(submission));
            case SINGLE_CHOICE -> evaluateSingleChoice(question, submission, answer);
            case ORDERING -> evaluateOrdering(submission, answer);
            case COMPONENT_BUILD -> evaluateComponentBuild(submission, answer);
            case TEXT_INPUT -> evaluateTextInput(submission, answer);
            case AUDIO -> throw new IllegalArgumentException("음성 문항은 recordings API로 제출해야 합니다.");
        };
    }

    public LearningResponseType responseType(JsonNode question) {
        String type = questionType(question);
        if ("FILL_IN_THE_BLANK".equals(type)) {
            return switch (question.path("content").path("inputType").asText()) {
                case "CHOICE" -> LearningResponseType.SINGLE_CHOICE;
                case "TEXT" -> LearningResponseType.TEXT_INPUT;
                case "VOICE" -> LearningResponseType.AUDIO;
                case "HANDWRITING" -> LearningResponseType.TRACE;
                default -> throw new IllegalStateException("빈칸 채우기 inputType이 올바르지 않습니다.");
            };
        }
        return switch (type) {
            case "VOWEL_TRACE", "CONSONANT_TRACE", "SYLLABLE_TRACE" -> LearningResponseType.TRACE;
            case "CONSONANT_SOUND_CHOICE", "VOWEL_SOUND_CHOICE",
                    "CONSONANT_VOWEL_CLASSIFICATION", "SYLLABLE_INITIAL_CHOICE",
                    "WORD_INITIAL_CHOICE", "SAME_INITIAL_WORD_CHOICE",
                    "FINAL_CONSONANT_CHOICE", "WORD_FINAL_SOUND_CHOICE",
                    "FINAL_CONSONANT_COMPARISON", "SIMILAR_SOUND_CHOICE",
                    "FINAL_CONSONANT_DELETE", "SYLLABLE_DELETE", "SYLLABLE_REPLACE",
                    "IMAGE_SENTENCE_MATCH" -> LearningResponseType.SINGLE_CHOICE;
            case "PHONEME_BLEND", "SYLLABLE_BLEND", "SENTENCE_ASSEMBLY" ->
                    LearningResponseType.ORDERING;
            case "BASIC_SYLLABLE_BUILD", "FINAL_SYLLABLE_BUILD", "DOUBLE_FINAL_BUILD" ->
                    LearningResponseType.COMPONENT_BUILD;
            case "WORD_READING", "NONWORD_READING", "DIFFICULT_WORD_PREVIEW",
                    "SENTENCE_READING", "SHORT_PASSAGE_READING", "SENTENCE_REPEAT",
                    "WORD_CHAIN_READING", "PHRASE_READING", "REPEATED_SENTENCE_READING",
                    "SHORT_STORY_READING" -> LearningResponseType.AUDIO;
            default -> throw new IllegalStateException("지원하지 않는 학습 문항 유형입니다: " + type);
        };
    }

    private Evaluation evaluateSingleChoice(
            JsonNode question,
            LearningSubmission submission,
            JsonNode answer
    ) {
        int selectedIndex = submission.response().path("selectedIndex").asInt();
        int expectedIndex = "SYLLABLE_DELETE".equals(questionType(question))
                ? answer.path("deleteIndex").asInt(Integer.MIN_VALUE)
                : answer.path("answerIndex").asInt(Integer.MIN_VALUE);
        requireExpected(expectedIndex != Integer.MIN_VALUE, "선택형 문항 정답");
        boolean correct = selectedIndex == expectedIndex;
        List<LearningErrorLocation> errors = correct
                ? List.of()
                : List.of(new LearningErrorLocation(selectedIndex, null, "INCORRECT_SELECTION"));
        ObjectNode correctResponse = responseValue(LearningResponseType.SINGLE_CHOICE);
        correctResponse.withObject("response").put("selectedIndex", expectedIndex);
        return new Evaluation(correct, correct ? 1000 : 0, errors, correctResponse);
    }

    private Evaluation evaluateOrdering(LearningSubmission submission, JsonNode answer) {
        JsonNode submitted = submission.response().path("orderedIndexes");
        JsonNode expected = answer.path("answerOrder");
        requireExpected(expected.isArray(), "순서형 문항 정답");
        boolean correct = submitted.equals(expected);
        List<LearningErrorLocation> errors = new ArrayList<>();
        if (!correct) {
            int size = Math.max(submitted.size(), expected.size());
            for (int index = 0; index < size; index++) {
                if (index >= submitted.size()
                        || index >= expected.size()
                        || submitted.get(index).asInt() != expected.get(index).asInt()) {
                    errors.add(new LearningErrorLocation(index, null, "INCORRECT_ORDER"));
                }
            }
        }
        ObjectNode correctResponse = responseValue(LearningResponseType.ORDERING);
        correctResponse.withObject("response").set("orderedIndexes", expected.deepCopy());
        return new Evaluation(correct, correct ? 1000 : 0, List.copyOf(errors), correctResponse);
    }

    private Evaluation evaluateComponentBuild(LearningSubmission submission, JsonNode answer) {
        ObjectNode expectedBySlot = objectMapper.createObjectNode();
        putExpectedSlot(expectedBySlot, answer, "INITIAL", "initialAnswerIndex", true);
        putExpectedSlot(expectedBySlot, answer, "MEDIAL", "medialAnswerIndex", true);
        putExpectedSlot(expectedBySlot, answer, "FINAL", "finalAnswerIndex", false);

        ObjectNode submittedBySlot = objectMapper.createObjectNode();
        submission.response().path("selections").forEach(selection ->
                submittedBySlot.put(
                        selection.path("slot").asText(),
                        selection.path("selectedIndex").asInt()
                ));
        boolean correct = submittedBySlot.equals(expectedBySlot);
        List<LearningErrorLocation> errors = new ArrayList<>();
        if (!correct) {
            int index = 0;
            for (String slot : List.of("INITIAL", "MEDIAL", "FINAL")) {
                if (expectedBySlot.has(slot) != submittedBySlot.has(slot)
                        || (expectedBySlot.has(slot)
                        && submittedBySlot.path(slot).asInt(Integer.MIN_VALUE)
                        != expectedBySlot.path(slot).asInt())) {
                    errors.add(new LearningErrorLocation(index, null, "INCORRECT_COMPONENT"));
                }
                index++;
            }
        }
        ObjectNode correctResponse = responseValue(LearningResponseType.COMPONENT_BUILD);
        ArrayNode selections = correctResponse.withObject("response").putArray("selections");
        for (String slot : List.of("INITIAL", "MEDIAL", "FINAL")) {
            if (expectedBySlot.has(slot)) {
                ObjectNode selection = selections.addObject();
                selection.put("slot", slot);
                selection.put("selectedIndex", expectedBySlot.path(slot).asInt());
            }
        }
        return new Evaluation(correct, correct ? 1000 : 0, List.copyOf(errors), correctResponse);
    }

    private Evaluation evaluateTextInput(LearningSubmission submission, JsonNode answer) {
        String submitted = normalizeText(submission.response().path("text").asText());
        JsonNode acceptedAnswers = answer.path("acceptedAnswers");
        requireExpected(acceptedAnswers.isArray() && !acceptedAnswers.isEmpty(), "직접 입력 문항 정답");
        List<String> accepted = new ArrayList<>();
        acceptedAnswers.forEach(value -> accepted.add(normalizeText(value.asText())));
        boolean correct = accepted.contains(submitted);
        List<LearningErrorLocation> errors = correct
                ? List.of()
                : List.of(new LearningErrorLocation(null, null, "INCORRECT_TEXT"));
        ObjectNode correctResponse = responseValue(LearningResponseType.TEXT_INPUT);
        correctResponse.withObject("response").put("text", acceptedAnswers.get(0).asText());
        return new Evaluation(correct, correct ? 1000 : 0, errors, correctResponse);
    }

    private ObjectNode correctTraceResponse(LearningSubmission submission) {
        ObjectNode result = responseValue(LearningResponseType.TRACE);
        result.set("response", submission.response().deepCopy());
        return result;
    }

    private ObjectNode responseValue(LearningResponseType type) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("responseType", type.name());
        result.putObject("response");
        return result;
    }

    private void validateResponse(LearningSubmission submission) {
        JsonNode response = submission.response();
        if (!response.isObject()) {
            throw new IllegalArgumentException("response는 JSON 객체여야 합니다.");
        }
        switch (submission.responseType()) {
            case TRACE -> validateTrace(response);
            case SINGLE_CHOICE -> {
                requireOnlyFields(response, Set.of("selectedIndex"));
                requireNonNegativeInteger(response, "selectedIndex");
            }
            case ORDERING -> validateOrdering(response);
            case COMPONENT_BUILD -> validateSelections(response);
            case TEXT_INPUT -> {
                requireOnlyFields(response, Set.of("text"));
                String text = response.path("text").asText();
                if (!response.path("text").isTextual() || text.isBlank() || text.length() > 1000) {
                    throw new IllegalArgumentException("text는 1자 이상 1000자 이하이어야 합니다.");
                }
            }
            case AUDIO -> throw new IllegalArgumentException("음성 문항은 recordings API로 제출해야 합니다.");
        }
    }

    private void validateTrace(JsonNode response) {
        requireOnlyFields(response, Set.of("canvasWidth", "canvasHeight", "strokes"));
        requirePositiveInteger(response, "canvasWidth");
        requirePositiveInteger(response, "canvasHeight");
        JsonNode strokes = response.path("strokes");
        if (!strokes.isArray() || strokes.isEmpty()) {
            throw new IllegalArgumentException("strokes는 하나 이상이어야 합니다.");
        }
        for (JsonNode stroke : strokes) {
            requireOnlyFields(stroke, Set.of("points"));
            JsonNode points = stroke.path("points");
            if (!points.isArray() || points.size() < 2) {
                throw new IllegalArgumentException("각 stroke에는 두 개 이상의 points가 필요합니다.");
            }
            for (JsonNode point : points) {
                requireOnlyFields(point, Set.of("x", "y", "elapsedMs", "pressure"));
                requireNonNegativeNumber(point, "x");
                requireNonNegativeNumber(point, "y");
                requireNonNegativeInteger(point, "elapsedMs");
                if (point.hasNonNull("pressure")) {
                    double pressure = point.path("pressure").asDouble(Double.NaN);
                    if (Double.isNaN(pressure) || pressure < 0 || pressure > 1) {
                        throw new IllegalArgumentException("pressure는 0 이상 1 이하이어야 합니다.");
                    }
                }
            }
        }
    }

    private void validateOrdering(JsonNode response) {
        requireOnlyFields(response, Set.of("orderedIndexes"));
        JsonNode indexes = response.path("orderedIndexes");
        if (!indexes.isArray() || indexes.isEmpty()) {
            throw new IllegalArgumentException("orderedIndexes는 하나 이상이어야 합니다.");
        }
        Set<Integer> unique = new HashSet<>();
        indexes.forEach(value -> {
            if (!value.isIntegralNumber() || value.asInt() < 0 || !unique.add(value.asInt())) {
                throw new IllegalArgumentException("orderedIndexes는 중복 없는 0 이상의 정수 배열이어야 합니다.");
            }
        });
    }

    private void validateSelections(JsonNode response) {
        requireOnlyFields(response, Set.of("selections"));
        JsonNode selections = response.path("selections");
        if (!selections.isArray() || selections.size() < 2 || selections.size() > 3) {
            throw new IllegalArgumentException("selections는 2개 이상 3개 이하이어야 합니다.");
        }
        Set<String> slots = new HashSet<>();
        selections.forEach(selection -> {
            requireOnlyFields(selection, Set.of("slot", "selectedIndex"));
            String slot = selection.path("slot").asText();
            if (!Set.of("INITIAL", "MEDIAL", "FINAL").contains(slot) || !slots.add(slot)) {
                throw new IllegalArgumentException("selections의 slot은 중복될 수 없습니다.");
            }
            requireNonNegativeInteger(selection, "selectedIndex");
        });
    }

    private void putExpectedSlot(
            ObjectNode target,
            JsonNode answer,
            String slot,
            String field,
            boolean required
    ) {
        if (answer.path(field).isIntegralNumber()) {
            target.put(slot, answer.path(field).asInt());
        } else if (required) {
            throw new IllegalStateException("구성형 문항 정답이 올바르지 않습니다.");
        }
    }

    private ObjectNode studentContent(JsonNode question) {
        JsonNode raw = question.path("content");
        ObjectNode content;
        if (raw instanceof ObjectNode object) {
            content = object.deepCopy();
        } else {
            content = objectMapper.createObjectNode();
            if (question.hasNonNull("text")) {
                content.put("sentence", question.path("text").asText());
            }
        }
        ANSWER_FIELDS.forEach(content::remove);
        return content;
    }

    private JsonNode answer(JsonNode question) {
        if (question.path("answer").isObject()) {
            return question.path("answer");
        }
        return question.path("content");
    }

    private void enrichImageUrls(ObjectNode content) {
        if (aiClient == null) {
            return;
        }
        addImageUrl(content);
        JsonNode choices = content.path("choices");
        if (choices.isArray()) {
            choices.forEach(choice -> {
                if (choice instanceof ObjectNode object) {
                    addImageUrl(object);
                }
            });
        }
    }

    private void addImageUrl(ObjectNode value) {
        String prompt = value.path("imagePrompt").asText();
        if (prompt.isBlank() || value.hasNonNull("imageUrl")) {
            return;
        }
        String requestId = "training-image-" + UUID.nameUUIDFromBytes(
                prompt.getBytes(StandardCharsets.UTF_8)
        );
        value.put(
                "imageUrl",
                aiClient.generateImage(new GenerateImageRequest(requestId, prompt, null)).imageUrl()
        );
    }

    /**
     * 학습자 App recordingTargets 규칙과 동일하게 계산한 권장 녹음 대상.
     * 시선 단어 지표의 targetIndex·토큰 기준 문장이 되므로, 녹음 없이 완료되는
     * 문항의 단어 시도 로그도 이 대상을 기준으로 남겨야 시선 병합과 좌표가 일치한다.
     */
    public RecommendedRecordingTarget recommendedRecordingTarget(JsonNode question) {
        JsonNode targets = question.path("analysisTargets");
        if (!targets.isArray() || targets.isEmpty()) {
            return null;
        }
        String recommendedText = recommendedRecordingText(answer(question));
        for (int index = 0; index < targets.size(); index++) {
            if (recommendedText.equals(targets.path(index).path("text").asText())) {
                return new RecommendedRecordingTarget(
                        index,
                        targets.path(index).path("text").asText()
                );
            }
        }
        return new RecommendedRecordingTarget(0, targets.path(0).path("text").asText());
    }

    public record RecommendedRecordingTarget(int targetIndex, String text) {
    }

    private void addRecordingTargets(ObjectNode result, JsonNode question) {
        boolean voiceRequired = false;
        for (JsonNode input : question.path("requiredInputs")) {
            voiceRequired |= "VOICE".equals(input.asText());
        }
        if (!voiceRequired) {
            return;
        }
        ArrayNode targets = result.putArray("recordingTargets");
        JsonNode sourceTargets = question.path("analysisTargets");
        for (int index = 0; index < sourceTargets.size(); index++) {
            ObjectNode target = targets.addObject();
            target.put("targetIndex", index);
            target.put("text", sourceTargets.path(index).path("text").asText());
        }
        String recommendedText = recommendedRecordingText(answer(question));
        for (int index = 0; index < targets.size(); index++) {
            if (recommendedText.equals(targets.path(index).path("text").asText())) {
                result.put("recommendedRecordingTargetIndex", index);
                return;
            }
        }
        if (!targets.isEmpty()) {
            result.put("recommendedRecordingTargetIndex", 0);
        }
    }

    private String recommendedRecordingText(JsonNode answer) {
        for (String field : List.of("expectedText", "result", "completedSentence", "target")) {
            if (answer.path(field).isTextual() && !answer.path(field).asText().isBlank()) {
                return answer.path(field).asText();
            }
        }
        return "";
    }

    private String questionType(JsonNode question) {
        String type = question.path("type").asText(question.path("questionType").asText());
        if (type.isBlank() && question.hasNonNull("text")) {
            return "SENTENCE_READING";
        }
        if (type.isBlank()) {
            throw new IllegalStateException("학습 문항 유형이 없습니다.");
        }
        return type;
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void requireExpected(boolean condition, String label) {
        if (!condition) {
            throw new IllegalStateException(label + "이 저장되어 있지 않습니다.");
        }
    }

    private void requirePositiveInteger(JsonNode node, String field) {
        if (!node.path(field).isIntegralNumber() || node.path(field).asInt() < 1) {
            throw new IllegalArgumentException(field + "는 1 이상의 정수이어야 합니다.");
        }
    }

    private void requireNonNegativeInteger(JsonNode node, String field) {
        if (!node.path(field).isIntegralNumber() || node.path(field).asInt() < 0) {
            throw new IllegalArgumentException(field + "는 0 이상의 정수이어야 합니다.");
        }
    }

    private void requireNonNegativeNumber(JsonNode node, String field) {
        if (!node.path(field).isNumber() || node.path(field).asDouble() < 0) {
            throw new IllegalArgumentException(field + "는 0 이상의 숫자이어야 합니다.");
        }
    }

    private void requireOnlyFields(JsonNode node, Set<String> allowed) {
        if (!allowed.containsAll(node.propertyNames())) {
            throw new IllegalArgumentException("response에 허용되지 않은 필드가 있습니다.");
        }
    }

    public record Evaluation(
            boolean correct,
            int totalScore,
            List<LearningErrorLocation> errorLocations,
            JsonNode correctResponse
    ) {
    }
}
