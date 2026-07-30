package com.iread.backend.training.input;

import com.iread.backend.training.generation.TrainingType;
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class TrainingInputPolicy {

    private TrainingInputPolicy() {
    }

    public static Set<TrainingInputType> expectedFor(TrainingType type) {
        EnumSet<TrainingInputType> inputs = EnumSet.noneOf(TrainingInputType.class);
        if (requiresVoice(type)) {
            inputs.add(TrainingInputType.VOICE);
        }
        if (requiresGaze(type)) {
            inputs.add(TrainingInputType.GAZE);
        }
        return Collections.unmodifiableSet(inputs);
    }

    public static Set<TrainingInputType> parseAndValidate(
            TrainingType type,
            JsonNode requiredInputs
    ) {
        Set<TrainingInputType> parsed = parse(requiredInputs);
        Set<TrainingInputType> expected = expectedFor(type);
        if (!parsed.equals(expected)) {
            throw new IllegalArgumentException(
                    type + " requiredInputs는 " + expected + "이어야 합니다."
            );
        }
        return parsed;
    }

    public static Set<TrainingInputType> resolve(
            TrainingType type,
            JsonNode requiredInputs
    ) {
        if (requiredInputs == null || requiredInputs.isMissingNode()) {
            return expectedFor(type);
        }
        return parseAndValidate(type, requiredInputs);
    }

    public static Set<TrainingInputType> forQuestion(JsonNode question) {
        TrainingType type = TrainingType.from(question.path("type").asText());
        return resolve(type, question.path("requiredInputs"));
    }

    private static Set<TrainingInputType> parse(JsonNode requiredInputs) {
        if (!requiredInputs.isArray()) {
            throw new IllegalArgumentException("requiredInputs는 배열이어야 합니다.");
        }
        EnumSet<TrainingInputType> parsed = EnumSet.noneOf(TrainingInputType.class);
        for (JsonNode input : requiredInputs) {
            if (!input.isTextual() || input.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "requiredInputs에는 VOICE 또는 GAZE 문자열만 사용할 수 있습니다."
                );
            }
            TrainingInputType type;
            try {
                type = TrainingInputType.valueOf(input.asText());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "지원하지 않는 requiredInputs 값입니다: " + input.asText()
                );
            }
            if (!parsed.add(type)) {
                throw new IllegalArgumentException(
                        "requiredInputs 값은 중복할 수 없습니다: " + type
                );
            }
        }
        return Collections.unmodifiableSet(parsed);
    }

    private static boolean requiresVoice(TrainingType type) {
        return switch (type) {
            case CONSONANT_SOUND_CHOICE, VOWEL_SOUND_CHOICE,
                    CONSONANT_VOWEL_CLASSIFICATION, SYLLABLE_INITIAL_CHOICE,
                    WORD_INITIAL_CHOICE, SAME_INITIAL_WORD_CHOICE,
                    FINAL_CONSONANT_CHOICE, WORD_FINAL_SOUND_CHOICE,
                    FINAL_CONSONANT_COMPARISON, SIMILAR_SOUND_CHOICE -> false;
            default -> true;
        };
    }

    private static boolean requiresGaze(TrainingType type) {
        return switch (type) {
            case VOWEL_TRACE, CONSONANT_TRACE, SYLLABLE_TRACE,
                    WORD_READING, NONWORD_READING, DIFFICULT_WORD_PREVIEW,
                    SENTENCE_READING, SHORT_PASSAGE_READING,
                    SENTENCE_ASSEMBLY, FILL_IN_THE_BLANK, IMAGE_SENTENCE_MATCH,
                    SENTENCE_REPEAT, WORD_CHAIN_READING, PHRASE_READING,
                    REPEATED_SENTENCE_READING, SHORT_STORY_READING -> true;
            default -> false;
        };
    }
}
