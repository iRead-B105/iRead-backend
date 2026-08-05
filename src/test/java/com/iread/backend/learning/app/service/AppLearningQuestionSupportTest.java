package com.iread.backend.learning.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.learning.app.dto.LearningResponseType;
import com.iread.backend.learning.app.dto.LearningSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.iread.backend.training.generation.TrainingType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AppLearningQuestionSupportTest {

    private ObjectMapper objectMapper;
    private AppLearningQuestionSupport support;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        support = new AppLearningQuestionSupport(objectMapper);
    }

    @Test
    void mapsStudentQuestionWithAnswerForFrontendHint() throws Exception {
        var question = objectMapper.readTree("""
                {
                  "type":"CONSONANT_SOUND_CHOICE",
                  "content":{
                    "audioText":"ㄱ",
                    "choices":["ㄱ","ㄴ"],
                    "answerIndex":0
                  },
                  "answer":{"answerIndex":0},
                  "analysisTargets":[{"text":"비공개"}]
                }
                """);

        var result = support.toStudentQuestion(question);

        assertThat(result.path("questionType").asText())
                .isEqualTo("CONSONANT_SOUND_CHOICE");
        assertThat(result.path("responseType").asText()).isEqualTo("SINGLE_CHOICE");
        assertThat(result.path("content").has("answerIndex")).isFalse();
        assertThat(result.path("answer").path("answerIndex").asInt()).isZero();
        assertThat(result.has("analysisTargets")).isFalse();
    }

    @Test
    void preservesImagePromptWithoutGeneratingImageAndAddsVoiceRecordingTargets() throws Exception {
        AiClient aiClient = mock(AiClient.class);
        support = new AppLearningQuestionSupport(objectMapper, aiClient);
        var question = objectMapper.readTree("""
                {
                  "type":"IMAGE_SENTENCE_MATCH",
                  "requiredInputs":["VOICE","GAZE"],
                  "content":{
                    "imagePrompt":"우산을 쓰는 아이",
                    "choices":["비가 와요.","눈이 와요."]
                  },
                  "answer":{"answerIndex":0,"completedSentence":"비가 와요."},
                  "analysisTargets":[
                    {"text":"비가 와요."},
                    {"text":"눈이 와요."}
                  ]
                }
                """);

        var result = support.toStudentQuestion(question);

        assertThat(result.path("content").has("imageUrl")).isFalse();
        assertThat(result.path("content").path("imagePrompt").asText())
                .isEqualTo("우산을 쓰는 아이");
        verifyNoInteractions(aiClient);
        assertThat(result.path("recordingTargets")).hasSize(2);
        assertThat(result.path("recommendedRecordingTargetIndex").asInt()).isZero();
    }

    @Test
    void evaluatesSingleChoiceFromStoredAnswer() throws Exception {
        var question = objectMapper.readTree("""
                {
                  "type":"CONSONANT_SOUND_CHOICE",
                  "content":{"audioText":"ㄱ","choices":["ㄱ","ㄴ"]},
                  "answer":{"answerIndex":0}
                }
                """);
        var response = objectMapper.createObjectNode().put("selectedIndex", 1);

        var result = support.evaluate(
                question,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.SINGLE_CHOICE,
                        response
                )
        );

        assertThat(result.correct()).isFalse();
        assertThat(result.totalScore()).isZero();
        assertThat(result.errorLocations()).singleElement()
                .extracting("errorCode")
                .isEqualTo("INCORRECT_SELECTION");
        assertThat(result.correctResponse().path("response").path("selectedIndex").asInt())
                .isZero();
    }

    @Test
    void evaluatesOrderingComponentAndTextResponses() throws Exception {
        var orderingQuestion = objectMapper.readTree("""
                {"type":"PHONEME_BLEND","answer":{"answerOrder":[1,0]}}
                """);
        var orderingResponse = objectMapper.createObjectNode();
        orderingResponse.putArray("orderedIndexes").add(1).add(0);
        assertThat(support.evaluate(
                orderingQuestion,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.ORDERING,
                        orderingResponse
                )
        ).correct()).isTrue();

        var componentQuestion = objectMapper.readTree("""
                {
                  "type":"BASIC_SYLLABLE_BUILD",
                  "answer":{"initialAnswerIndex":1,"medialAnswerIndex":0}
                }
                """);
        var componentResponse = objectMapper.createObjectNode();
        var selections = componentResponse.putArray("selections");
        selections.addObject().put("slot", "INITIAL").put("selectedIndex", 1);
        selections.addObject().put("slot", "MEDIAL").put("selectedIndex", 0);
        assertThat(support.evaluate(
                componentQuestion,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.COMPONENT_BUILD,
                        componentResponse
                )
        ).correct()).isTrue();

        var textQuestion = objectMapper.readTree("""
                {
                  "type":"FILL_IN_THE_BLANK",
                  "content":{"inputType":"TEXT"},
                  "answer":{"acceptedAnswers":["푸른 하늘"]}
                }
                """);
        var textResponse = objectMapper.createObjectNode().put("text", "  푸른   하늘 ");
        assertThat(support.evaluate(
                textQuestion,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.TEXT_INPUT,
                        textResponse
                )
        ).correct()).isTrue();
    }

    @Test
    void acceptsStructurallyValidTraceForDeterministicDemo() throws Exception {
        var question = objectMapper.readTree("""
                {"type":"VOWEL_TRACE","answer":{"target":"ㅏ"}}
                """);
        var response = objectMapper.createObjectNode();
        response.put("canvasWidth", 100);
        response.put("canvasHeight", 100);
        var points = response.putArray("strokes").addObject().putArray("points");
        points.addObject().put("x", 1).put("y", 2).put("elapsedMs", 0);
        points.addObject().put("x", 3).put("y", 4).put("elapsedMs", 10);

        var result = support.evaluate(
                question,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.TRACE,
                        response
                )
        );

        assertThat(result.correct()).isTrue();
        assertThat(result.totalScore()).isEqualTo(1000);
    }

    @Test
    void rejectsResponseTypeMismatch() throws Exception {
        var question = objectMapper.readTree("""
                {"type":"CONSONANT_SOUND_CHOICE","answer":{"answerIndex":0}}
                """);

        assertThatThrownBy(() -> support.evaluate(
                question,
                new LearningSubmission(
                        UUID.randomUUID(),
                        LearningResponseType.TEXT_INPUT,
                        objectMapper.createObjectNode().put("text", "ㄱ")
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("responseType");
    }

    @ParameterizedTest
    @EnumSource(TrainingType.class)
    void resolvesResponseTypeForEveryGeneratedTrainingType(TrainingType trainingType) {
        var question = objectMapper.createObjectNode().put("type", trainingType.name());
        if (trainingType == TrainingType.FILL_IN_THE_BLANK) {
            question.putObject("content").put("inputType", "TEXT");
        }

        assertThat(support.responseType(question)).isNotNull();
    }
}
