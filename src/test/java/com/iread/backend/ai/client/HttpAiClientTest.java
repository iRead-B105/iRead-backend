package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.req.StoryHistoryLine;
import com.iread.backend.ai.dto.req.StoryBranchInputReviewRequest;
import com.iread.backend.ai.dto.req.StoryTemplateData;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.global.audio.TemporaryAudioStorage;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAiClientTest {

    private final JsonMapper objectMapper = new JsonMapper();

    @TempDir
    Path tempDir;

    private MockRestServiceServer server;
    private HttpAiClient aiClient;
    private FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        fileStorage = org.mockito.Mockito.mock(FileStorage.class);
        aiClient = new HttpAiClient(
                builder.baseUrl("http://localhost:8081").build(),
                new AiClientProperties(
                        URI.create("http://localhost:8081"),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        "test-api-key",
                        false,
                        false,
                        false,
                        null,
                        null,
                        null
                ),
                new MockTrainingGenerator(objectMapper),
                new MockTrainingEvaluator(),
                new MockStoryGenerator(),
                new MockSpeechProcessor(),
                new TemporaryAudioStorage(
                        tempDir.resolve("audio").toString(),
                        new AudioUploadPolicy(
                                DataSize.ofMegabytes(20),
                                "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                        )
                ),
                fileStorage
        );
    }

    @Test
    void 이야기_분기_STT_원문을_경량_검토_API로_보낸다() {
        StoryBranchInputReviewRequest request = new StoryBranchInputReviewRequest(
                "review-1",
                "토끼는 이제 무엇을 할까요?",
                List.of("다리를 건너요", "친구를 불러요", "숲으로 돌아가요"),
                "강을 따라가 볼래요"
        );
        server.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/story/branch-input/review"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "review-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "requestId": "review-1",
                          "question": "토끼는 이제 무엇을 할까요?",
                          "options": ["다리를 건너요", "친구를 불러요", "숲으로 돌아가요"],
                          "transcript": "강을 따라가 볼래요"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "requestId": "review-1",
                          "decision": "ALLOW",
                          "reasonCode": "OK",
                          "policyVersion": "story-branch-input-v1"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.reviewStoryBranchInput(request);

        assertThat(response.decision().name()).isEqualTo("ALLOW");
        assertThat(response.reasonCode().name()).isEqualTo("OK");
        server.verify();
    }

    @Test
    void 훈련_생성_요청을_보내고_JSON_결과를_반환한다() throws Exception {
        GenerateTrainingRequest request = request("request-1");
        server.expect(once(), requestTo("http://localhost:8081/api/v1/trainings/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "request-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "requestId": "request-1",
                          "trainingId": 10,
                          "studentId": 20,
                          "trainingTemplateId": 30,
                          "schemaVersion": 1,
                          "inputData": {
                            "expectedWords": ["사과", "바나나"]
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "requestId": "request-1",
                          "schemaVersion": 1,
                          "generatedData": {
                            "questions": [{"question": "사과를 읽어보세요."}]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.generateTraining(request);

        assertThat(response.requestId()).isEqualTo("request-1");
        assertThat(response.schemaVersion()).isEqualTo(1);
        assertThat(response.generatedData().path("questions").get(0).path("question").asString())
                .isEqualTo("사과를 읽어보세요.");
        server.verify();
    }

    @Test
    void 이미지_생성_결과를_백엔드_스토리지에_보관한다() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        server.expect(once(), requestTo("http://localhost:8081/api/v1/images/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "image-request-1"))
                .andRespond(withSuccess("""
                        {
                          "requestId": "image-request-1",
                          "imageUrl": "/api/v1/images/mock/example.svg",
                          "provider": "MOCK_IMAGE_V1"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://localhost:8081/api/v1/images/mock/example.svg"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(png, MediaType.IMAGE_PNG));
        org.mockito.Mockito.when(fileStorage.store(
                "ai-image-request-1.png", "image/png", png
        )).thenReturn(new StoredFile(
                "ai-image-request-1.png", "stored.png", png.length, "/uploads/images/stored.png"
        ));

        var response = aiClient.generateImage(new GenerateImageRequest(
                "image-request-1",
                "우산을 쓰는 아이"
        ));

        assertThat(response.imageUrl())
                .isEqualTo("/uploads/images/stored.png");
        assertThat(response.provider()).isEqualTo("MOCK_IMAGE_V1");
        server.verify();
    }

    @Test
    void mockGenerate이면_외부_AI_호출_없이_인라인_이미지를_반환한다() {
        HttpAiClient mockClient = client(
                RestClient.builder().baseUrl("http://localhost:8081").build(),
                true
        );

        var response = mockClient.generateImage(new GenerateImageRequest(
                "image-request-mock",
                "우산을 쓰는 아이"
        ));

        assertThat(response.requestId()).isEqualTo("image-request-mock");
        assertThat(response.provider()).isEqualTo("BACKEND_MOCK_IMAGE_PNG_V1");
        assertThat(response.imageUrl()).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(response.imageUrl().substring(
                "data:image/png;base64,".length()
        ));
        assertThat(png).startsWith(0x89, 0x50, 0x4e, 0x47);
        server.verify();
    }

    @Test
    void 이야기_주인공_요청이면_이야기_친구용_목_이미지를_반환한다() {
        HttpAiClient mockClient = client(
                RestClient.builder().baseUrl("http://localhost:8081").build(),
                true
        );

        var response = mockClient.generateImage(new GenerateImageRequest(
                "story-character-image",
                "[STORY_CHARACTER] 별빛 숲의 친구 주인공"
        ));

        assertThat(response.provider()).isEqualTo("BACKEND_MOCK_STORY_CHARACTER_PNG_V1");
        assertThat(response.imageUrl()).startsWith("data:image/png;base64,");
        server.verify();
    }

    @Test
    void AI_서버의_오류_상태를_클라이언트_예외로_변환한다() throws Exception {
        server.expect(requestTo("http://localhost:8081/api/v1/trainings/generate"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"generation failed\"}"));

        assertThatThrownBy(() -> aiClient.generateTraining(request("request-2")))
                .isInstanceOfSatisfying(AiClientException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(500);
                    assertThat(exception.getMessage()).isEqualTo("AI 서버가 오류 응답을 반환했습니다.");
                });
        server.verify();
    }

    @Test
    void 응답의_requestId가_다르면_예외가_발생한다() throws Exception {
        server.expect(requestTo("http://localhost:8081/api/v1/trainings/generate"))
                .andRespond(withSuccess("""
                        {
                          "requestId": "different-request",
                          "schemaVersion": 1,
                          "generatedData": {"questions": []}
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.generateTraining(request("request-3")))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서버 응답의 requestId가 요청과 일치하지 않습니다.");
        server.verify();
    }

    @Test
    void generatedData가_JSON_객체가_아니면_예외가_발생한다() throws Exception {
        server.expect(requestTo("http://localhost:8081/api/v1/trainings/generate"))
                .andRespond(withSuccess("""
                        {
                          "requestId": "request-4",
                          "schemaVersion": 1,
                          "generatedData": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.generateTraining(request("request-4")))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서버 응답의 generatedData는 JSON 객체여야 합니다.");
        server.verify();
    }

    @Test
    void 최초_스토리_생성을_요청하고_선택지까지의_대사를_반환한다() {
        GenerateStoryRequest request = new GenerateStoryRequest(
                "story-request-1",
                100L,
                20L,
                1,
                0,
                new StoryTemplateData(30L, "신비한 숲", "숲에서 친구를 만나는 이야기")
        );
        server.expect(requestTo("http://localhost:8081/api/v1/story/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "story-request-1"))
                .andRespond(withSuccess("""
                        {
                          "requestId": "story-request-1",
                          "schemaVersion": 1,
                          "nextProgress": 50,
                          "completed": false,
                          "lines": [
                            {"content": "숲에 도착했어요.", "requiresBranchInput": false, "branchPrompt": null},
                            {
                              "content": "어디로 갈까요?",
                              "requiresBranchInput": true,
                              "branchPrompt": {
                                "options": [
                                  {"optionNo": 1, "label": "별빛 길로 간다"},
                                  {"optionNo": 2, "label": "숲길로 간다"},
                                  {"optionNo": 3, "label": "시냇물 길로 간다"}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.generateStory(request);

        assertThat(response.completed()).isFalse();
        assertThat(response.lines()).extracting("content")
                .containsExactly("숲에 도착했어요.", "어디로 갈까요?");
        assertThat(response.lines().getLast().requiresBranchInput()).isTrue();
        assertThat(response.lines().getLast().branchPrompt().options())
                .extracting(option -> option.optionNo())
                .containsExactly(1, 2, 3);
        server.verify();
    }

    @Test
    void 자연어_선택지와_이전_내용으로_다음_스토리_생성을_요청한다() {
        ContinueStoryRequest request = new ContinueStoryRequest(
                "story-request-2",
                100L,
                20L,
                1,
                50,
                new StoryTemplateData(30L, "신비한 숲", "숲에서 친구를 만나는 이야기"),
                1001L,
                "노랫소리를 따라간다",
                List.of(new StoryHistoryLine(1001L, "어디로 갈까요?", true))
        );
        server.expect(requestTo("http://localhost:8081/api/v1/story/continue"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "branchIntent": "노랫소리를 따라간다",
                          "currentStoryLineId": 1001,
                          "currentProgress": 50
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "requestId": "story-request-2",
                          "schemaVersion": 1,
                          "nextProgress": 100,
                          "completed": true,
                          "lines": [
                            {"content": "친구를 만나 집으로 돌아왔어요.", "requiresBranchInput": false}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.continueStory(request);

        assertThat(response.completed()).isTrue();
        assertThat(response.lines().getFirst().content()).isEqualTo("친구를 만나 집으로 돌아왔어요.");
        server.verify();
    }

    @Test
    void sendsMultipartSpeechAndReturnsTranscription() throws Exception {
        server.expect(requestTo("http://localhost:8081/api/v1/speech/transcribe"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "speech-request-1"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("""
                        {
                          "requestId": "speech-request-1",
                          "transcript": "책을 읽어요",
                          "confidence": 0.95,
                          "durationMs": 1200
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.transcribeSpeech(
                "speech-request-1",
                20L,
                "책을 읽어요",
                new MockMultipartFile(
                        "audioFile", "reading.webm", "audio/webm", new byte[]{1, 2, 3}
                )
        );

        assertThat(response.transcript()).isEqualTo("책을 읽어요");
        assertThat(response.confidence()).isEqualTo(0.95);
        try (var files = Files.list(tempDir.resolve("audio"))) {
            assertThat(files.toList()).isEmpty();
        }
        server.verify();
    }

    @Test
    void sendsSentenceAudioAndReturnsWordPronunciationScores() {
        server.expect(requestTo(
                        "http://localhost:8081/api/v1/speech/pronunciation/analyze"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "pronunciation-request-1"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.MULTIPART_FORM_DATA
                ))
                .andRespond(withSuccess("""
                        {
                          "requestId": "pronunciation-request-1",
                          "pronunciationAccuracyScore": 88.0,
                          "fluencyScore": 81.0,
                          "completenessScore": 100.0,
                          "pronScore": 86.0,
                          "confidence": 0.95,
                          "analysisVersion": "AZURE_SPEECH_V1",
                          "words": [
                            {
                              "resultIndex": 0,
                              "word": "아기는",
                              "accuracyScore": 91.0,
                              "errorType": "None",
                              "offsetMs": 100,
                              "durationMs": 500
                            },
                            {
                              "resultIndex": 1,
                              "word": "사과를",
                              "accuracyScore": 85.0,
                              "errorType": "None",
                              "offsetMs": 650,
                              "durationMs": 600
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.analyzePronunciation(
                new PronunciationAnalysisRequest(
                        "pronunciation-request-1",
                        "아기는 사과를",
                        "sentence.wav",
                        "audio/wav",
                        new byte[]{1, 2, 3}
                )
        );

        assertThat(response.words()).hasSize(2);
        assertThat(response.words().get(1).word()).isEqualTo("사과를");
        assertThat(response.words().get(1).accuracyScore()).isEqualTo(85.0);
        server.verify();
    }

    @Test
    void 발음_분석_기준_문장을_UTF_8로_전송한다() {
        // 기준 문장이 깨져 전달되면 Azure가 전부 Omission으로 채점한다.
        server.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/speech/pronunciation/analyze"
                ))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("아기는 사과를")))
                .andRespond(withSuccess("""
                        {
                          "requestId": "utf8-request",
                          "pronunciationAccuracyScore": 88.0,
                          "confidence": 0.95,
                          "analysisVersion": "AZURE_SPEECH_KO_KR_WORD_V1",
                          "words": [
                            {
                              "resultIndex": 0,
                              "word": "아기는",
                              "accuracyScore": 91.0,
                              "errorType": "None",
                              "offsetMs": 0,
                              "durationMs": 400
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.analyzePronunciation(new PronunciationAnalysisRequest(
                "utf8-request", "아기는 사과를", "sentence.webm", "audio/webm", new byte[]{1, 2, 3}
        ));

        assertThat(response.words().getFirst().word()).isEqualTo("아기는");
        server.verify();
    }

    @Test
    void 발음_평가만_AI_서버로_보내고_STT와_TTS는_mock을_유지한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer speechServer = MockRestServiceServer.bindTo(builder).build();
        HttpAiClient client = new HttpAiClient(
                builder.baseUrl("http://localhost:8081").build(),
                new AiClientProperties(
                        URI.create("http://localhost:8081"),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        "test-api-key",
                        true,
                        true,
                        true,
                        false,
                        null,
                        null
                ),
                new MockTrainingGenerator(objectMapper),
                new MockTrainingEvaluator(),
                new MockStoryGenerator(),
                new MockSpeechProcessor(),
                new TemporaryAudioStorage(
                        tempDir.resolve("mixed-audio").toString(),
                        new AudioUploadPolicy(
                                DataSize.ofMegabytes(20),
                                "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                        )
                ),
                org.mockito.Mockito.mock(FileStorage.class)
        );
        speechServer.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/speech/pronunciation/analyze"
                ))
                .andRespond(withSuccess("""
                        {
                          "requestId": "mixed-mode-1",
                          "pronunciationAccuracyScore": 88.0,
                          "confidence": 0.95,
                          "analysisVersion": "AZURE_SPEECH_KO_KR_WORD_V1",
                          "words": [
                            {
                              "resultIndex": 0,
                              "word": "사과",
                              "accuracyScore": 88.0,
                              "errorType": "None",
                              "offsetMs": 0,
                              "durationMs": 400
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var analysis = client.analyzePronunciation(new PronunciationAnalysisRequest(
                "mixed-mode-1", "사과", "word.wav", "audio/wav", new byte[]{1, 2, 3}
        ));
        var transcription = client.transcribeSpeech(
                "mixed-mode-2",
                20L,
                "책을 읽어요",
                new MockMultipartFile(
                        "audioFile", "reading.webm", "audio/webm", new byte[]{1, 2, 3}
                )
        );
        var tts = client.synthesizeSpeech(
                new SpeechSynthesisRequest("mixed-mode-3", "책을 읽어요", null)
        );

        assertThat(analysis.analysisVersion()).isEqualTo("AZURE_SPEECH_KO_KR_WORD_V1");
        assertThat(transcription.transcript()).isEqualTo("책을 읽어요");
        assertThat(tts.audio()).startsWith((byte) 'I', (byte) 'D', (byte) '3');
        speechServer.verify();
    }

    @Test
    void 잘못된_JSON_응답은_통신_예외로_변환하고_재시도하지_않는다() throws Exception {
        server.expect(once(), requestTo("http://localhost:8081/api/v1/trainings/generate"))
                .andRespond(withSuccess("{invalid-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.generateTraining(request("malformed-response")))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서버와 통신하는 데 실패했습니다.");

        server.verify();
    }

    @Test
    void 음성_인식이_실패해도_임시_파일을_삭제하고_재시도하지_않는다() throws Exception {
        server.expect(once(), requestTo("http://localhost:8081/api/v1/speech/transcribe"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> aiClient.transcribeSpeech(
                "speech-failure",
                20L,
                "책을 읽어요",
                new MockMultipartFile(
                        "audioFile", "reading.webm", "audio/webm", new byte[]{1, 2, 3}
                )
        ))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서버가 음성 인식 오류를 반환했습니다.");

        try (var files = Files.list(tempDir.resolve("audio"))) {
            assertThat(files.toList()).isEmpty();
        }
        server.verify();
    }

    @Test
    void sendsTtsRequestAndReturnsAudioWithDurationHeader() {
        byte[] audio = new byte[]{'I', 'D', '3'};
        server.expect(requestTo("http://localhost:8081/api/v1/speech/synthesize"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "tts-request-1"))
                .andExpect(content().json("""
                        {"requestId":"tts-request-1","text":"책을 읽어요","voice":null}
                        """))
                .andRespond(withSuccess()
                        .body(audio)
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .header("X-Request-Id", "tts-request-1")
                        .header("X-Audio-Duration-Ms", "1500"));

        var response = aiClient.synthesizeSpeech(
                new SpeechSynthesisRequest("tts-request-1", "책을 읽어요", null)
        );

        assertThat(response.audio()).isEqualTo(audio);
        assertThat(response.durationMs()).isEqualTo(1500);
        server.verify();
    }

    @Test
    void sendsTrainingResultAndReturnsAccuracy() throws Exception {
        EvaluateTrainingRequest request = new EvaluateTrainingRequest(
                "training-evaluation-10",
                10L,
                20L,
                30L,
                1,
                objectMapper.readTree("""
                        {"questions":[{"questionId":"q1","selectedAnswer":"apple"}]}
                        """)
        );
        server.expect(once(), requestTo("http://localhost:8081/api/v1/trainings/evaluate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "training-evaluation-10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "requestId": "training-evaluation-10",
                          "trainingId": 10,
                          "studentId": 20,
                          "trainingTemplateId": 30,
                          "schemaVersion": 1,
                          "result": {
                            "questions": [
                              {"questionId": "q1", "selectedAnswer": "apple"}
                            ]
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "requestId": "training-evaluation-10",
                          "schemaVersion": 1,
                          "accuracy": 87.25
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.evaluateTraining(request);

        assertThat(response.accuracy()).isEqualByComparingTo("87.25");
        server.verify();
    }

    @Test
    void rejectsAccuracyOutsideZeroToOneHundred() throws Exception {
        EvaluateTrainingRequest request = new EvaluateTrainingRequest(
                "training-evaluation-10",
                10L,
                20L,
                30L,
                1,
                objectMapper.readTree("{}")
        );
        server.expect(requestTo("http://localhost:8081/api/v1/trainings/evaluate"))
                .andRespond(withSuccess("""
                        {
                          "requestId": "training-evaluation-10",
                          "schemaVersion": 1,
                          "accuracy": 100.01
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.evaluateTraining(request))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 훈련 평가 응답의 accuracy는 0.0 이상 100.0 이하여야 합니다.");
        server.verify();
    }

    private GenerateTrainingRequest request(String requestId) throws Exception {
        return new GenerateTrainingRequest(
                requestId,
                10L,
                20L,
                30L,
                1,
                objectMapper.readTree("{\"expectedWords\":[\"사과\",\"바나나\"]}")
        );
    }

    private HttpAiClient client(RestClient restClient, boolean mockGenerate) {
        return new HttpAiClient(
                restClient,
                new AiClientProperties(
                        URI.create("http://localhost:8081"),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        "test-api-key",
                        mockGenerate,
                        false,
                        false,
                        null,
                        null,
                        null
                ),
                new MockTrainingGenerator(objectMapper),
                new MockTrainingEvaluator(),
                new MockStoryGenerator(),
                new MockSpeechProcessor(),
                new TemporaryAudioStorage(
                        tempDir.resolve("audio").toString(),
                        new AudioUploadPolicy(
                                DataSize.ofMegabytes(20),
                                "audio/webm,audio/wav,audio/mpeg,audio/mp4"
                        )
                ),
                org.mockito.Mockito.mock(FileStorage.class)
        );
    }
}
