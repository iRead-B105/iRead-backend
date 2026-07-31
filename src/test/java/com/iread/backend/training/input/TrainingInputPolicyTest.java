package com.iread.backend.training.input;

import com.iread.backend.training.generation.TrainingType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingInputPolicyTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void validatesRequiredInputsForAllSeedTemplates() throws Exception {
        JsonNode root;
        try (var input = getClass().getClassLoader()
                .getResourceAsStream("training-templates.json")) {
            root = objectMapper.readTree(input);
        }

        assertThat(root.path("templates")).hasSize(37);
        for (JsonNode template : root.path("templates")) {
            JsonNode prompt = template.path("prompt");
            TrainingType type = TrainingType.from(prompt.path("trainingType").asText());

            assertThat(TrainingInputPolicy.parseAndValidate(
                    type,
                    prompt.path("requiredInputs")
            )).isEqualTo(TrainingInputPolicy.expectedFor(type));
        }
    }

    @Test
    void rejectsUnsupportedDuplicateAndMismatchedInputs() throws Exception {
        assertThatThrownBy(() -> TrainingInputPolicy.parseAndValidate(
                TrainingType.SENTENCE_READING,
                objectMapper.readTree("[\"CAMERA\"]")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는");

        assertThatThrownBy(() -> TrainingInputPolicy.parseAndValidate(
                TrainingType.SENTENCE_READING,
                objectMapper.readTree("[\"VOICE\",\"VOICE\"]")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");

        assertThatThrownBy(() -> TrainingInputPolicy.parseAndValidate(
                TrainingType.SENTENCE_READING,
                objectMapper.readTree("[\"VOICE\"]")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GAZE");
    }

    @Test
    void restoresLegacyQuestionPolicyFromTrainingType() throws Exception {
        JsonNode question = objectMapper.readTree("""
                {"questionNo":1,"type":"SYLLABLE_BLEND"}
                """);

        assertThat(TrainingInputPolicy.forQuestion(question))
                .isEqualTo(Set.of(TrainingInputType.VOICE));
    }
}
