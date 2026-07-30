package com.iread.backend.training.generation;

import com.iread.backend.training.analysis.HangulSyllable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingCandidateContractTest {

    private static final Set<TrainingType> THREE_CHOICE_TYPES = Set.of(
            TrainingType.CONSONANT_SOUND_CHOICE,
            TrainingType.VOWEL_SOUND_CHOICE,
            TrainingType.SYLLABLE_INITIAL_CHOICE,
            TrainingType.WORD_INITIAL_CHOICE,
            TrainingType.SAME_INITIAL_WORD_CHOICE,
            TrainingType.FINAL_CONSONANT_CHOICE,
            TrainingType.WORD_FINAL_SOUND_CHOICE,
            TrainingType.FINAL_CONSONANT_COMPARISON
    );

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
            if (THREE_CHOICE_TYPES.contains(type)) {
                assertThat(prompt.path("additionalPrompt").asText())
                        .contains("정확히 3개");
                response.data().forEach(candidate ->
                        assertThat(candidate.path("choices"))
                                .as(type + " choices")
                                .hasSize(3)
                );
            }
            if (type == TrainingType.FINAL_CONSONANT_DELETE) {
                response.data().forEach(candidate -> {
                    HangulSyllable syllable = HangulSyllable.decompose(
                            candidate.path("source").asText().charAt(0)
                    );
                    List<String> units = new java.util.ArrayList<>();
                    candidate.path("removableUnits").forEach(
                            value -> units.add(value.asText())
                    );
                    assertThat(units).containsExactly(
                            syllable.onset(),
                            syllable.vowel(),
                            syllable.coda()
                    );
                    assertThat(candidate.path("answerIndex").asInt()).isEqualTo(2);
                });
            }
        }
    }

    @Test
    void rejectsFinalDeleteDistractorCardsInsteadOfSourceComponents() throws Exception {
        JsonNode root;
        try (var input = getClass().getClassLoader().getResourceAsStream("training-templates.json")) {
            root = objectMapper.readTree(input);
        }
        JsonNode prompt = root.path("templates").get(18).path("prompt");
        TrainingCandidateRequest request = new TrainingCandidateRequest(
                "invalid-final-delete",
                2,
                TrainingType.FINAL_CONSONANT_DELETE,
                5,
                2,
                List.of(),
                List.of(),
                prompt.path("additionalPrompt").asText(),
                prompt.path("outputTemplate")
        );
        TrainingCandidateResponse response = provider.generate(request);
        var invalid = (tools.jackson.databind.node.ObjectNode) response.data().get(0);
        invalid.set(
                "removableUnits",
                objectMapper.createArrayNode().add("ㄱ").add("ㄴ").add("ㅁ")
        );
        invalid.put("answerIndex", 0);

        CandidateValidationResult result = validator.validate(request, response);

        assertThat(result.issues()).extracting(CandidateValidationIssue::type)
                .contains("INVALID_FINAL_DELETE_UNITS");
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
                .contains(
                        "COUNT_MISMATCH",
                        "DUPLICATE_OPTION",
                        "INVALID_INDEX",
                        "INVALID_CHOICE_COUNT"
                );
    }
}
