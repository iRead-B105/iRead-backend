package com.iread.backend.training.generation;

import com.iread.backend.training.analysis.KoreanG2pEngine;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.input.TrainingInputPolicy;
import com.iread.backend.training.input.TrainingInputType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingQuestionAssemblerTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private final TrainingQuestionAssembler assembler = new TrainingQuestionAssembler(
            objectMapper,
            new KoreanTextAnalyzer(new KoreanG2pEngine())
    );

    @Test
    void analyzesAnswerTextThatWasRemovedFromContent() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "cards": ["먹는다.", "아기는", "사과를"],
                  "answerOrder": [1, 2, 0],
                  "completedSentence": "아기는 사과를 먹는다."
                }
                """);

        TrainingQuestionAssembler.AssembledQuestion assembled = assembler.assemble(
                1,
                TrainingType.SENTENCE_ASSEMBLY,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE, TrainingInputType.GAZE)
        );
        JsonNode question = assembled.question();

        assertThat(question.path("content").has("completedSentence")).isFalse();
        assertThat(paths(question)).containsExactly(
                "$.content.cards[0]",
                "$.content.cards[1]",
                "$.content.cards[2]",
                "$.answer.completedSentence"
        );
        assertThat(textAt(question, "$.answer.completedSentence")).isEqualTo("아기는 사과를 먹는다.");
        assertThat(featureCodesAt(question, "$.answer.completedSentence")).contains(
                "WORD.SYLLABLE_COUNT.3",
                "PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ"
        );
        assertThat(assembled.featureCodes()).contains("SENTENCE.SIMPLE");
    }

    @Test
    void appendsAnswerTargetsAfterContentTargetsSoStoredIndexesStayStable() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "audioParts": ["ㄱ", "ㅏ"],
                  "cards": ["ㄱ", "ㅏ", "ㄹ"],
                  "answerOrder": [0, 1],
                  "result": "가"
                }
                """);

        JsonNode question = assembler.assemble(
                1,
                TrainingType.PHONEME_BLEND,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE)
        ).question();

        assertThat(paths(question)).containsExactly(
                "$.content.audioParts[0]",
                "$.content.audioParts[1]",
                "$.content.cards[0]",
                "$.content.cards[1]",
                "$.content.cards[2]",
                "$.answer.result"
        );
        assertThat(textAt(question, "$.answer.result")).isEqualTo("가");
    }

    @Test
    void analyzesEveryAnswerTextInArrayFields() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "sentence": "책상 위에 {{blank}} 그림이 있다.",
                  "inputType": "TEXT",
                  "choices": [],
                  "answerIndex": -1,
                  "acceptedAnswers": ["사과", "능금"],
                  "completedSentence": "책상 위에 사과 그림이 있다."
                }
                """);

        JsonNode question = assembler.assemble(
                1,
                TrainingType.FILL_IN_THE_BLANK,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE, TrainingInputType.GAZE)
        ).question();

        assertThat(paths(question)).containsExactly(
                "$.content.sentence",
                "$.answer.acceptedAnswers[0]",
                "$.answer.acceptedAnswers[1]",
                "$.answer.completedSentence"
        );
    }

    @Test
    void skipsAnswerTextThatIsAlreadyAnalyzedFromContent() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "targetAudioText": "가",
                  "initialChoices": ["ㄱ", "ㄴ", "ㅁ"],
                  "medialChoices": ["ㅏ", "ㅓ", "ㅗ"],
                  "initialAnswerIndex": 0,
                  "medialAnswerIndex": 0,
                  "result": "가"
                }
                """);

        JsonNode question = assembler.assemble(
                1,
                TrainingType.BASIC_SYLLABLE_BUILD,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE)
        ).question();

        assertThat(question.path("answer").path("result").asText()).isEqualTo("가");
        assertThat(paths(question)).doesNotContain("$.answer.result");
        assertThat(paths(question)).startsWith("$.content.targetAudioText");
    }

    @Test
    void skipsAnswerFieldsThatContentKeeps() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "vowelType": "BASIC",
                  "target": "ㅏ",
                  "soundText": "ㅏ",
                  "traceAssetKey": "vowel_a"
                }
                """);

        JsonNode question = assembler.assemble(
                1,
                TrainingType.VOWEL_TRACE,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE, TrainingInputType.GAZE)
        ).question();

        assertThat(question.path("content").path("target").asText()).isEqualTo("ㅏ");
        assertThat(question.path("answer").path("target").asText()).isEqualTo("ㅏ");
        assertThat(paths(question)).containsExactly("$.content.target", "$.content.soundText");
    }

    @Test
    void keepsReadingTypesUnchangedBecauseExpectedTextIsDerived() {
        JsonNode candidate = objectMapper.readTree("""
                {
                  "readingOrder": "SEQUENTIAL",
                  "words": ["사과", "나무", "바다"]
                }
                """);

        JsonNode question = assembler.assemble(
                1,
                TrainingType.WORD_READING,
                candidate,
                List.of(),
                Set.of(TrainingInputType.VOICE, TrainingInputType.GAZE)
        ).question();

        assertThat(question.path("answer").path("expectedText").asText())
                .isEqualTo("사과 나무 바다");
        assertThat(paths(question)).containsExactly(
                "$.content.words[0]",
                "$.content.words[1]",
                "$.content.words[2]",
                "$.answer.recordingText"
        );
        assertThat(question.path("words")).hasSize(3);
    }

    @Test
    void keepsContentTargetsFirstAndAnswerTextsUniqueForAllTrainingTypes() throws Exception {
        JsonNode root;
        try (var input = getClass().getClassLoader().getResourceAsStream("training-templates.json")) {
            root = objectMapper.readTree(input);
        }
        DeterministicTrainingCandidateProvider provider =
                new DeterministicTrainingCandidateProvider(objectMapper);

        assertThat(root.path("templates")).hasSize(34);
        for (JsonNode template : root.path("templates")) {
            JsonNode prompt = template.path("prompt");
            TrainingType type = TrainingType.from(prompt.path("trainingType").asText());
            TrainingCandidateResponse response = provider.generate(new TrainingCandidateRequest(
                    "assembler-" + type,
                    2,
                    type,
                    5,
                    2,
                    List.of(),
                    List.of(),
                    prompt.path("additionalPrompt").asText(),
                    prompt.path("outputTemplate")
            ));

            JsonNode question = assembler.assemble(
                    1,
                    type,
                    response.data().get(0),
                    List.of(),
                    TrainingInputPolicy.resolve(type, prompt.path("requiredInputs"))
            ).question();

            List<String> paths = paths(question);
            assertThat(paths).as(type + " analysis targets").isNotEmpty();
            List<String> contentTexts = new ArrayList<>();
            List<String> answerTexts = new ArrayList<>();
            boolean answerSectionStarted = false;
            for (String path : paths) {
                boolean answerPath = path.startsWith("$.answer.");
                if (answerPath) {
                    answerSectionStarted = true;
                    answerTexts.add(textAt(question, path));
                } else {
                    assertThat(answerSectionStarted)
                            .as(type + " keeps content targets before answer targets")
                            .isFalse();
                    contentTexts.add(textAt(question, path));
                }
            }
            assertThat(answerTexts)
                    .as(type + " answer texts are not analyzed twice")
                    .doesNotContainAnyElementsOf(contentTexts)
                    .doesNotHaveDuplicates();
        }
    }

    private List<String> paths(JsonNode question) {
        List<String> paths = new ArrayList<>();
        question.path("analysisTargets").forEach(target -> paths.add(target.path("path").asText()));
        return paths;
    }

    private String textAt(JsonNode question, String path) {
        return target(question, path).path("text").asText();
    }

    private List<String> featureCodesAt(JsonNode question, String path) {
        List<String> codes = new ArrayList<>();
        target(question, path).path("featureCodes").forEach(code -> codes.add(code.asText()));
        return codes;
    }

    private JsonNode target(JsonNode question, String path) {
        for (JsonNode target : question.path("analysisTargets")) {
            if (path.equals(target.path("path").asText())) {
                return target;
            }
        }
        throw new AssertionError("분석 대상을 찾을 수 없습니다: " + path);
    }
}
