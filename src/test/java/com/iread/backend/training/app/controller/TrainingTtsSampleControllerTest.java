package com.iread.backend.training.app.controller;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.iread.backend.ai.client.HttpAiClient;
import com.iread.backend.ai.client.MockSpeechProcessor;
import com.iread.backend.ai.client.MockStoryGenerator;
import com.iread.backend.ai.client.MockTrainingEvaluator;
import com.iread.backend.ai.client.MockTrainingGenerator;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.global.audio.TemporaryAudioStorage;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingTtsSampleControllerTest {

    private static final long STUDENT_ID = 2002L;
    private static final long TRAINING_ID = 181021L;
    private static final String GENERATED_DATA = """
            {
              "schemaVersion": 2,
              "questions": [
                {
                  "questionNo": 1,
                  "type": "SENTENCE_REPEAT",
                  "requiredInputs": ["VOICE", "GAZE"],
                  "content": {"sentence": "아기는 사과를 먹는다.", "emotion": "HAPPY"},
                  "answer": {"expectedText": "아기는 사과를 먹는다."},
                  "analysisTargets": [
                    {"path": "$.content.sentence", "text": "아기는 사과를 먹는다.", "featureCodes": []}
                  ]
                }
              ]
            }
            """;

    private TrainingDataRepository trainingDataRepository;
    private AiClient aiClient;
    private TrainingTtsSampleController controller;

    @BeforeEach
    void setUp() {
        trainingDataRepository = mock(TrainingDataRepository.class);
        aiClient = mock(AiClient.class);
        controller = new TrainingTtsSampleController(
                trainingDataRepository,
                new StudentResourceAccessPolicy(),
                aiClient,
                new JsonMapper()
        );
        when(aiClient.synthesizeSpeech(any()))
                .thenReturn(new SpeechSynthesisResponse(new byte[]{'I', 'D', '3'}, 2_637L));
    }

    private void givenQuestion(String generatedData) {
        TrainingDataEntity data = new TrainingDataEntity(
                mock(TrainingEntity.class),
                generatedData
        );
        when(trainingDataRepository.findByTrainingId(TRAINING_ID))
                .thenReturn(Optional.of(data));
    }

    @Test
    void 문항의_기준_문장을_TTS_음성으로_돌려준다() {
        givenQuestion(GENERATED_DATA);

        var response = controller.ttsSample(STUDENT_ID, STUDENT_ID, TRAINING_ID, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getHeaders().getFirst("X-Audio-Duration-Ms")).isEqualTo("2637");
    }

    @Test
    void demo_프로필처럼_TTS가_mock이어도_음성을_돌려준다() {
        // 실제 HttpAiClient는 ai.mock-speech=true면 목 음성을 만든다.
        JsonMapper mapper = new JsonMapper();
        HttpAiClient realClient = new HttpAiClient(
                RestClient.builder().baseUrl("http://localhost:8081").build(),
                new AiClientProperties(
                        URI.create("http://localhost:8081"),
                        Duration.ofSeconds(1), Duration.ofSeconds(1), "",
                        true, true, true, null, null, null
                ),
                new MockTrainingGenerator(mapper),
                new MockTrainingEvaluator(),
                new MockStoryGenerator(),
                new MockSpeechProcessor(),
                new TemporaryAudioStorage(
                        Path.of(System.getProperty("java.io.tmpdir"), "tts-test").toString(),
                        new AudioUploadPolicy(DataSize.ofMegabytes(20), "audio/webm,audio/wav")
                )
        );
        TrainingTtsSampleController mockedTts = new TrainingTtsSampleController(
                trainingDataRepository, new StudentResourceAccessPolicy(), realClient, mapper
        );
        givenQuestion(GENERATED_DATA);

        var response = mockedTts.ttsSample(STUDENT_ID, STUDENT_ID, TRAINING_ID, 1);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void 문항이_없으면_찾을_수_없다고_알린다() {
        when(trainingDataRepository.findByTrainingId(TRAINING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.ttsSample(STUDENT_ID, STUDENT_ID, TRAINING_ID, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 읽을_문장이_없으면_500이_아니라_찾을_수_없다고_알린다() {
        // 읽기 문항이 아니면 answer.expectedText가 없다. 서버 오류로 새면 안 된다.
        givenQuestion("""
                {"schemaVersion": 2, "questions": [
                  {"questionNo": 1, "type": "CONSONANT_SOUND_CHOICE",
                   "content": {"audioText": "ㄱ"}, "answer": {"answerIndex": 0}}
                ]}
                """);

        assertThatThrownBy(() -> controller.ttsSample(STUDENT_ID, STUDENT_ID, TRAINING_ID, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
