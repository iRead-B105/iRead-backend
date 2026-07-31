package com.iread.backend.training.generation;

import com.iread.backend.training.analysis.AnalyzedWord;
import com.iread.backend.training.analysis.FeatureOccurrence;
import com.iread.backend.training.analysis.KoreanTextAnalysis;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.input.TrainingInputType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TrainingQuestionAssembler {

    private static final Pattern KOREAN_TEXT = Pattern.compile(".*[가-힣ㄱ-ㅎㅏ-ㅣ].*");
    private static final Set<String> ANALYSIS_EXCLUDED_ANSWER_FIELDS = Set.of("expectedText");

    private final ObjectMapper objectMapper;
    private final KoreanTextAnalyzer analyzer;

    public AssembledQuestion assemble(
            int questionNo,
            TrainingType type,
            JsonNode candidate,
            List<String> targetFeatureCodes,
            Set<TrainingInputType> requiredInputs
    ) {
        if (!candidate.isObject()) {
            throw new IllegalArgumentException("훈련 후보 문항은 JSON 객체여야 합니다.");
        }
        ObjectNode content = (ObjectNode) candidate.deepCopy();
        ObjectNode answer = buildAnswer(type, candidate, content);
        List<TextTarget> textTargets = new ArrayList<>();
        collectTextTargets(content, "$.content", textTargets);
        collectAnswerTextTargets(answer, content, textTargets);

        ArrayNode analysisTargets = objectMapper.createArrayNode();
        LinkedHashSet<String> allFeatureCodes = new LinkedHashSet<>();
        List<KoreanTextAnalysis> analyses = new ArrayList<>();
        for (TextTarget target : textTargets) {
            KoreanTextAnalysis analysis = analyzer.analyze(target.text());
            analyses.add(analysis);
            ObjectNode node = analysisTargets.addObject();
            node.put("path", target.path());
            node.put("text", target.text());
            ArrayNode featureCodes = node.putArray("featureCodes");
            analysis.words().stream().flatMap(word -> word.featureCodes().stream())
                    .distinct()
                    .forEach(code -> {
                        featureCodes.add(code);
                        allFeatureCodes.add(code);
                    });
            analysis.sentenceFeatureCodes().forEach(code -> {
                if (allFeatureCodes.add(code)) {
                    featureCodes.add(code);
                }
            });
        }

        ObjectNode question = objectMapper.createObjectNode();
        question.put("questionNo", questionNo);
        question.put("type", type.name());
        ArrayNode inputArray = question.putArray("requiredInputs");
        requiredInputs.stream()
                .sorted()
                .map(Enum::name)
                .forEach(inputArray::add);
        question.set("content", content);
        question.set("answer", answer);
        question.set("analysisTargets", analysisTargets);
        ArrayNode targetCodes = question.putArray("targetFeatureCodes");
        targetFeatureCodes.forEach(targetCodes::add);

        if (isReadingType(type)) {
            String primaryText = primaryText(type, candidate);
            KoreanTextAnalysis primary = analyzer.analyze(primaryText);
            question.put("text", primaryText);
            question.set("words", words(primary.words()));
            primary.words().forEach(word -> allFeatureCodes.addAll(word.featureCodes()));
            analyses.add(primary);
        }
        return new AssembledQuestion(question, Set.copyOf(allFeatureCodes), List.copyOf(analyses));
    }

    private ObjectNode buildAnswer(TrainingType type, JsonNode candidate, ObjectNode content) {
        ObjectNode answer = objectMapper.createObjectNode();
        for (String field : answerFields(type)) {
            if (candidate.has(field)) {
                answer.set(field, candidate.get(field).deepCopy());
                if (answerExclusiveFields(type).contains(field)) {
                    content.remove(field);
                }
            }
        }
        if (answer.isEmpty()) {
            answer.put("expectedText", primaryText(type, candidate));
        }
        return answer;
    }

    private Set<String> answerFields(TrainingType type) {
        return switch (type) {
            case VOWEL_TRACE, CONSONANT_TRACE, SYLLABLE_TRACE -> Set.of("target");
            case CONSONANT_SOUND_CHOICE, VOWEL_SOUND_CHOICE, CONSONANT_VOWEL_CLASSIFICATION,
                    SYLLABLE_INITIAL_CHOICE, WORD_INITIAL_CHOICE, SAME_INITIAL_WORD_CHOICE,
                    FINAL_CONSONANT_CHOICE, WORD_FINAL_SOUND_CHOICE, FINAL_CONSONANT_COMPARISON,
                    SIMILAR_SOUND_CHOICE, IMAGE_SENTENCE_MATCH -> Set.of("answerIndex");
            case PHONEME_BLEND, SYLLABLE_BLEND -> Set.of("answerOrder", "result");
            case BASIC_SYLLABLE_BUILD -> Set.of(
                    "initialAnswerIndex", "medialAnswerIndex", "result"
            );
            case FINAL_SYLLABLE_BUILD, DOUBLE_FINAL_BUILD -> Set.of(
                    "initialAnswerIndex", "medialAnswerIndex", "finalAnswerIndex", "result"
            );
            case FINAL_CONSONANT_DELETE -> Set.of("answerIndex", "result");
            case SYLLABLE_DELETE -> Set.of("deleteIndex", "result");
            case SYLLABLE_REPLACE -> Set.of("replaceIndex", "answerIndex", "result");
            case SENTENCE_ASSEMBLY -> Set.of("answerOrder", "completedSentence");
            case FILL_IN_THE_BLANK -> Set.of("answerIndex", "acceptedAnswers", "completedSentence");
            default -> Set.of();
        };
    }

    private Set<String> answerExclusiveFields(TrainingType type) {
        return switch (type) {
            case VOWEL_TRACE, CONSONANT_TRACE, SYLLABLE_TRACE -> Set.of();
            case SYLLABLE_REPLACE -> Set.of("answerIndex", "result");
            default -> answerFields(type);
        };
    }

    private void collectTextTargets(JsonNode node, String path, List<TextTarget> targets) {
        if (node.isTextual()) {
            String text = node.asText();
            if (KOREAN_TEXT.matcher(text).matches()) {
                targets.add(new TextTarget(path, text));
            }
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectTextTargets(node.get(index), path + "[" + index + "]", targets);
            }
            return;
        }
        if (node.isObject()) {
            node.properties().forEach(field ->
                    collectTextTargets(field.getValue(), path + "." + field.getKey(), targets));
        }
    }

    /**
     * `content`에서 제거된 정답 텍스트도 분석 대상에 넣는다. `content`에 남아 있는 필드와
     * 이미 수집한 텍스트는 `analysisTargets`가 같은 텍스트를 두 번 담지 않도록 건너뛴다.
     */
    private void collectAnswerTextTargets(
            ObjectNode answer,
            ObjectNode content,
            List<TextTarget> targets
    ) {
        Set<String> collectedTexts = new LinkedHashSet<>();
        targets.forEach(target -> collectedTexts.add(target.text()));
        answer.propertyNames().stream()
                .filter(field -> !ANALYSIS_EXCLUDED_ANSWER_FIELDS.contains(field))
                .filter(field -> !content.has(field))
                .sorted()
                .forEach(field -> {
                    List<TextTarget> answerTargets = new ArrayList<>();
                    collectTextTargets(answer.get(field), "$.answer." + field, answerTargets);
                    answerTargets.stream()
                            .filter(target -> collectedTexts.add(target.text()))
                            .forEach(targets::add);
                });
    }

    private boolean isReadingType(TrainingType type) {
        return switch (type) {
            case WORD_READING, NONWORD_READING, DIFFICULT_WORD_PREVIEW, SENTENCE_READING,
                    SHORT_PASSAGE_READING, SENTENCE_REPEAT, WORD_CHAIN_READING, PHRASE_READING,
                    REPEATED_SENTENCE_READING, SHORT_STORY_READING -> true;
            default -> false;
        };
    }

    private String primaryText(TrainingType type, JsonNode candidate) {
        if (candidate.hasNonNull("sentence")) {
            return candidate.path("sentence").asText();
        }
        if (type == TrainingType.SHORT_PASSAGE_READING) {
            return joinTextArray(candidate.path("sentences"));
        }
        if (type == TrainingType.SHORT_STORY_READING) {
            List<String> values = new ArrayList<>();
            candidate.path("sentences").forEach(value -> values.add(value.path("text").asText()));
            return String.join(" ", values);
        }
        if (type == TrainingType.NONWORD_READING) {
            List<String> values = new ArrayList<>();
            candidate.path("words").forEach(value -> values.add(value.path("text").asText()));
            return String.join(" ", values);
        }
        if (candidate.path("words").isArray()) {
            return joinTextArray(candidate.path("words"));
        }
        for (String field : List.of(
                "result", "target", "targetAudioText", "audioText", "source", "completedSentence"
        )) {
            if (candidate.hasNonNull(field) && KOREAN_TEXT.matcher(candidate.path(field).asText()).matches()) {
                return candidate.path(field).asText();
            }
        }
        return "읽기";
    }

    private String joinTextArray(JsonNode values) {
        List<String> texts = new ArrayList<>();
        values.forEach(value -> texts.add(value.asText()));
        return String.join(" ", texts);
    }

    private ArrayNode words(List<AnalyzedWord> words) {
        ArrayNode result = objectMapper.createArrayNode();
        for (AnalyzedWord word : words) {
            ObjectNode node = result.addObject();
            node.put("wordIndex", word.wordIndex());
            node.put("surface", word.surface());
            ArrayNode codes = node.putArray("featureCodes");
            word.featureCodes().forEach(codes::add);
            ArrayNode occurrences = node.putArray("featureOccurrences");
            for (FeatureOccurrence occurrence : word.featureOccurrences()) {
                ObjectNode item = occurrences.addObject();
                item.put("code", occurrence.code());
                item.put("startSyllableIndex", occurrence.startSyllableIndex());
                item.put("endSyllableIndex", occurrence.endSyllableIndex());
                item.put("orthographicForm", occurrence.orthographicForm());
                item.put("pronunciationForm", occurrence.pronunciationForm());
            }
        }
        return result;
    }

    public record AssembledQuestion(
            ObjectNode question,
            Set<String> featureCodes,
            List<KoreanTextAnalysis> analyses
    ) {
    }

    private record TextTarget(String path, String text) {
    }
}
