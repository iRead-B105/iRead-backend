package com.iread.backend.training.generation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingCandidateContractTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private final TrainingCandidateValidator validator = new TrainingCandidateValidator();
    private final DeterministicTrainingCandidateProvider provider =
            new DeterministicTrainingCandidateProvider(objectMapper);

    @Test
    void deterministicProviderProducesFiveValidCandidatesForAllTrainingTypes() throws Exception {
        JsonNode root;
        try (var input = getClass().getClassLoader().getResourceAsStream("training-templates.json")) {
            root = objectMapper.readTree(input);
        }

        assertThat(root.path("templates")).hasSize(34);
        for (JsonNode template : root.path("templates")) {
            JsonNode prompt = template.path("prompt");
            TrainingType type = TrainingType.from(prompt.path("trainingType").asText());
            TrainingCandidateRequest request = new TrainingCandidateRequest(
                    "contract-" + type,
                    2,
                    type,
                    5,
                    2,
                    List.of(),
                    List.of(),
                    prompt.path("additionalPrompt").asText(),
                    prompt.path("outputTemplate")
            );

            TrainingCandidateResponse response = provider.generate(request);
            CandidateValidationResult result = validator.validate(request, response);

            assertThat(result.issues())
                    .as(type + " validation issues")
                    .isEmpty();
            assertThat(response.data()).hasSize(5);
        }
    }

    @Test
    void rejectsWrongCountDuplicateOptionsAndInvalidIndex() throws Exception {
        JsonNode outputTemplate = objectMapper.readTree("""
                {
                  "type": "VOWEL_SOUND_CHOICE",
                  "data": [{
                    "audioText": "<string>",
                    "choices": ["<string>"],
                    "answerIndex": "<integer>"
                  }]
                }
                """);
        TrainingCandidateRequest request = new TrainingCandidateRequest(
                "invalid-contract",
                2,
                TrainingType.VOWEL_SOUND_CHOICE,
                5,
                1,
                List.of(),
                List.of(),
                "",
                outputTemplate
        );
        JsonNode data = objectMapper.readTree("""
                [
                  {"audioText":"ㅏ","choices":["ㅏ","ㅏ"],"answerIndex":2}
                ]
                """);

        CandidateValidationResult result = validator.validate(
                request,
                new TrainingCandidateResponse("VOWEL_SOUND_CHOICE", data)
        );

        assertThat(result.passed()).isFalse();
        assertThat(result.issues()).extracting(CandidateValidationIssue::type)
                .contains("COUNT_MISMATCH", "DUPLICATE_OPTION", "INVALID_INDEX");
    }
}
