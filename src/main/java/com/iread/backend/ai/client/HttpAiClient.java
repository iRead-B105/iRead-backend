package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateImageResponse;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.global.audio.TemporaryAudioStorage;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.StoredFile;
import com.iread.backend.pronunciation.DeterministicPronunciationAnalysisAdapter;
import com.iread.backend.pronunciation.PronunciationAnalysisRequest;
import com.iread.backend.pronunciation.PronunciationAnalysisResult;
import com.iread.backend.pronunciation.PronunciationWordResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Component
public class HttpAiClient implements AiClient {

    static final String GENERATE_TRAINING_PATH = "/api/v1/trainings/generate";
    static final String EVALUATE_TRAINING_PATH = "/api/v1/trainings/evaluate";
    static final String GENERATE_STORY_PATH = "/api/v1/story/generate";
    static final String CONTINUE_STORY_PATH = "/api/v1/story/continue";
    static final String TRANSCRIBE_SPEECH_PATH = "/api/v1/speech/transcribe";
    static final String ANALYZE_PRONUNCIATION_PATH = "/api/v1/speech/pronunciation/analyze";
    static final String SYNTHESIZE_SPEECH_PATH = "/api/v1/speech/synthesize";
    static final String GENERATE_IMAGE_PATH = "/api/v1/images/generate";

    private final RestClient restClient;
    private final AiClientProperties properties;
    private final MockTrainingGenerator mockTrainingGenerator;
    private final MockTrainingEvaluator mockTrainingEvaluator;
    private final MockStoryGenerator mockStoryGenerator;
    private final MockSpeechProcessor mockSpeechProcessor;
    private final TemporaryAudioStorage temporaryAudioStorage;
    private final FileStorage fileStorage;
    private final DeterministicPronunciationAnalysisAdapter mockPronunciationAnalyzer =
            new DeterministicPronunciationAnalysisAdapter();

    public HttpAiClient(
            @Qualifier("aiRestClient") RestClient restClient,
            AiClientProperties properties,
            MockTrainingGenerator mockTrainingGenerator,
            MockTrainingEvaluator mockTrainingEvaluator,
            MockStoryGenerator mockStoryGenerator,
            MockSpeechProcessor mockSpeechProcessor,
            TemporaryAudioStorage temporaryAudioStorage,
            FileStorage fileStorage
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mockTrainingGenerator = mockTrainingGenerator;
        this.mockTrainingEvaluator = mockTrainingEvaluator;
        this.mockStoryGenerator = mockStoryGenerator;
        this.mockSpeechProcessor = mockSpeechProcessor;
        this.temporaryAudioStorage = temporaryAudioStorage;
        this.fileStorage = fileStorage;
    }

    @Override
    public GenerateTrainingResponse generateTraining(GenerateTrainingRequest request) {
        if (properties.mockGenerate()) {
            return mockTrainingGenerator.generate(request);
        }
        try {
            GenerateTrainingResponse response = restClient.post()
                    .uri(GENERATE_TRAINING_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 오류 응답을 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(GenerateTrainingResponse.class);

            validateResponse(request, response);
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 통신하는 데 실패했습니다.", exception);
        }
    }

    @Override
    public EvaluateTrainingResponse evaluateTraining(EvaluateTrainingRequest request) {
        if (properties.mockEvaluate()) {
            return mockTrainingEvaluator.evaluate(request);
        }
        try {
            EvaluateTrainingResponse response = restClient.post()
                    .uri(EVALUATE_TRAINING_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 훈련 평가 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(EvaluateTrainingResponse.class);

            validateEvaluationResponse(request, response);
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 훈련 평가 통신 중 실패했습니다.", exception);
        }
    }

    @Override
    public GenerateStoryResponse generateStory(GenerateStoryRequest request) {
        if (properties.mockGenerate()) {
            return mockStoryGenerator.generate(request);
        }
        return requestStory(
                GENERATE_STORY_PATH,
                request,
                request.requestId(),
                request.schemaVersion(),
                request.currentProgress()
        );
    }

    @Override
    public GenerateStoryResponse continueStory(ContinueStoryRequest request) {
        if (properties.mockGenerate()) {
            return mockStoryGenerator.continueStory(request);
        }
        return requestStory(
                CONTINUE_STORY_PATH,
                request,
                request.requestId(),
                request.schemaVersion(),
                request.currentProgress()
        );
    }

    @Override
    public GenerateImageResponse generateImage(GenerateImageRequest request) {
        if (properties.mockGenerate()) {
            return mockImage(request);
        }
        try {
            GenerateImageResponse response = restClient.post()
                    .uri(GENERATE_IMAGE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 이미지 생성 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(GenerateImageResponse.class);
            if (!Objects.equals(request.requestId(), response.requestId())
                    || response.imageUrl() == null
                    || response.imageUrl().isBlank()) {
                throw new AiClientException("AI 이미지 생성 응답 값이 유효하지 않습니다.");
            }
            URI imageUri = properties.baseUrl().resolve(response.imageUrl());
            validateSameAiOrigin(imageUri);
            var imageResponse = restClient.get()
                    .uri(imageUri)
                    .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 생성 이미지 조회 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .toEntity(byte[].class);
            byte[] content = imageResponse.getBody();
            MediaType contentType = imageResponse.getHeaders().getContentType();
            if (content == null || content.length == 0 || contentType == null) {
                throw new AiClientException("AI 생성 이미지 본문 또는 형식이 없습니다.");
            }
            String extension = switch (contentType.toString()) {
                case "image/png" -> "png";
                case "image/jpeg" -> "jpg";
                default -> throw new AiClientException("AI 생성 이미지 형식이 유효하지 않습니다.");
            };
            StoredFile stored = fileStorage.store(
                    "ai-" + request.requestId() + "." + extension,
                    contentType.toString(),
                    content
            );
            return new GenerateImageResponse(
                    response.requestId(),
                    stored.url(),
                    response.provider()
            );
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 이미지 생성 통신 중 실패했습니다.", exception);
        }
    }

    private void validateSameAiOrigin(URI imageUri) {
        URI base = properties.baseUrl();
        if (!Objects.equals(base.getScheme(), imageUri.getScheme())
                || !Objects.equals(base.getAuthority(), imageUri.getAuthority())) {
            throw new AiClientException("AI 생성 이미지 URL의 출처가 유효하지 않습니다.");
        }
    }

    private GenerateImageResponse mockImage(GenerateImageRequest request) {
        String prompt = Objects.toString(request.prompt(), "그림").strip();
        if (prompt.startsWith("[STORY_CHARACTER]")) {
            return mockStoryCharacterImage(request, prompt);
        }
        if (prompt.length() > 42) {
            prompt = prompt.substring(0, 42) + "…";
        }
        String escapedPrompt = prompt
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="960" height="540"
                     viewBox="0 0 960 540">
                  <defs>
                    <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="#dff4ff"/>
                      <stop offset="1" stop-color="#fff8dc"/>
                    </linearGradient>
                  </defs>
                  <rect width="960" height="540" rx="32" fill="url(#sky)"/>
                  <circle cx="790" cy="105" r="58" fill="#ffd86b"/>
                  <path d="M0 390 Q180 300 360 390 T720 390 T1080 390 V540 H0Z"
                        fill="#9ed98f"/>
                  <rect x="120" y="145" width="720" height="230" rx="28"
                        fill="#ffffff" fill-opacity=".9"/>
                  <text x="480" y="245" text-anchor="middle"
                        font-family="sans-serif" font-size="38" font-weight="700"
                        fill="#31506b">그림과 문장을 연결해요</text>
                  <text x="480" y="305" text-anchor="middle"
                        font-family="sans-serif" font-size="25"
                        fill="#526b7f">%s</text>
                </svg>
                """.formatted(escapedPrompt);
        String encoded = Base64.getEncoder().encodeToString(
                svg.getBytes(StandardCharsets.UTF_8)
        );
        return new GenerateImageResponse(
                request.requestId(),
                "data:image/svg+xml;base64," + encoded,
                "BACKEND_MOCK_IMAGE_V1"
        );
    }

    private GenerateImageResponse mockStoryCharacterImage(
            GenerateImageRequest request,
            String prompt
    ) {
        String label = prompt
                .replaceFirst("^\\[STORY_CHARACTER\\]\\s*", "")
                .strip();
        if (label.length() > 28) {
            label = label.substring(0, 28) + "...";
        }
        String escapedLabel = label
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="768" height="768"
                     viewBox="0 0 768 768">
                  <defs>
                    <linearGradient id="night" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="#5546a9"/>
                      <stop offset="1" stop-color="#8e79d6"/>
                    </linearGradient>
                    <filter id="shadow">
                      <feDropShadow dx="0" dy="14" stdDeviation="18"
                                    flood-color="#33266f" flood-opacity=".35"/>
                    </filter>
                  </defs>
                  <rect width="768" height="768" rx="52" fill="url(#night)"/>
                  <g fill="#fff3a7" opacity=".95">
                    <circle cx="116" cy="116" r="8"/><circle cx="646" cy="140" r="10"/>
                    <circle cx="590" cy="86" r="5"/><circle cx="170" cy="204" r="5"/>
                    <path d="M676 246l7 15 16 2-12 11 3 16-14-8-14 8 3-16-12-11 16-2z"/>
                  </g>
                  <ellipse cx="384" cy="650" rx="230" ry="42" fill="#352a76" opacity=".38"/>
                  <g filter="url(#shadow)">
                    <path d="M244 272L278 126l112 102z" fill="#ef9f52"/>
                    <path d="M524 272L490 126 378 228z" fill="#ef9f52"/>
                    <path d="M269 231l24-74 58 62zM499 231l-24-74-58 62z" fill="#ffd4c2"/>
                    <ellipse cx="384" cy="398" rx="190" ry="184" fill="#f3aa59"/>
                    <path d="M239 378q145-100 290 0v156q-145 118-290 0z" fill="#fff0d0"/>
                    <ellipse cx="320" cy="392" rx="22" ry="28" fill="#3c315f"/>
                    <ellipse cx="448" cy="392" rx="22" ry="28" fill="#3c315f"/>
                    <circle cx="313" cy="383" r="7" fill="#fff"/>
                    <circle cx="441" cy="383" r="7" fill="#fff"/>
                    <path d="M366 444q18-18 36 0-18 24-36 0z" fill="#7c4c55"/>
                    <path d="M384 468q-34 36-68 2M384 468q34 36 68 2"
                          fill="none" stroke="#7c4c55" stroke-width="9"
                          stroke-linecap="round"/>
                    <path d="M242 521q142 116 284 0l-38 114H280z" fill="#6bd2bf"/>
                    <circle cx="384" cy="564" r="25" fill="#fff3a7"/>
                  </g>
                  <rect x="76" y="675" width="616" height="58" rx="29"
                        fill="#fff" fill-opacity=".92"/>
                  <text x="384" y="712" text-anchor="middle"
                        font-family="sans-serif" font-size="21" fill="#44377d">%s</text>
                </svg>
                """.formatted(escapedLabel);
        String encoded = Base64.getEncoder().encodeToString(
                svg.getBytes(StandardCharsets.UTF_8)
        );
        return new GenerateImageResponse(
                request.requestId(),
                "data:image/svg+xml;base64," + encoded,
                "BACKEND_MOCK_STORY_CHARACTER_V1"
        );
    }

    @Override
    public SpeechTranscriptionResponse transcribeSpeech(
            String requestId, Long studentId, String expectedText, MultipartFile audioFile
    ) {
        if (properties.transcribeMocked()) {
            return mockSpeechProcessor.transcribe(requestId, expectedText);
        }
        try (TemporaryAudioStorage.StagedAudio stagedAudio = temporaryAudioStorage.stage(audioFile)) {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("requestId", requestId);
            body.part("studentId", studentId.toString());
            if (expectedText != null && !expectedText.isBlank()) {
                body.part("expectedText", expectedText);
            }
            body.part("audioFile", new FileSystemResource(stagedAudio.path()) {
                @Override
                public String getFilename() {
                    return stagedAudio.originalFilename();
                }
            }).contentType(MediaType.parseMediaType(
                    stagedAudio.contentType()
            ));

            SpeechTranscriptionResponse response = restClient.post()
                    .uri(TRANSCRIBE_SPEECH_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", requestId)
                    .body(body.build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 음성 인식 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(SpeechTranscriptionResponse.class);
            if (!Objects.equals(requestId, response.requestId())) {
                throw new AiClientException("AI 음성 인식 응답의 requestId가 요청과 일치하지 않습니다.");
            }
            if (response.transcript() == null || response.confidence() < 0
                    || response.confidence() > 1 || response.durationMs() < 0) {
                throw new AiClientException("AI 음성 인식 응답 값이 유효하지 않습니다.");
            }
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 음성 인식 통신 중 실패했습니다.", exception);
        }
    }

    @Override
    public PronunciationAnalysisResult analyzePronunciation(
            PronunciationAnalysisRequest request
    ) {
        if (properties.pronunciationMocked()) {
            return mockPronunciationAnalyzer.analyze(request);
        }
        try {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("requestId", request.requestId());
            body.part("expectedText", request.expectedText());
            body.part("audioFile", new ByteArrayResource(request.audio()) {
                @Override
                public String getFilename() {
                    return request.originalFilename();
                }
            }).contentType(MediaType.parseMediaType(request.contentType()));

            PronunciationAnalysisResult response = restClient.post()
                    .uri(ANALYZE_PRONUNCIATION_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(body.build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 발음 분석 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(PronunciationAnalysisResult.class);
            validatePronunciationResponse(request, response);
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 발음 분석 통신 중 실패했습니다.", exception);
        }
    }

    @Override
    public SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request) {
        if (properties.ttsMocked()) {
            return mockSpeechProcessor.synthesize(request);
        }
        try {
            var response = restClient.post()
                    .uri(SYNTHESIZE_SPEECH_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 TTS 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .toEntity(byte[].class);
            String responseRequestId = response.getHeaders().getFirst("X-Request-Id");
            if (!Objects.equals(request.requestId(), responseRequestId)) {
                throw new AiClientException("AI TTS 응답의 requestId가 요청과 일치하지 않습니다.");
            }
            String durationHeader = response.getHeaders().getFirst("X-Audio-Duration-Ms");
            if (durationHeader == null || response.getBody() == null || response.getBody().length == 0) {
                throw new AiClientException("AI TTS 응답에 음성 또는 재생 시간이 없습니다.");
            }
            long durationMs = Long.parseLong(durationHeader);
            if (durationMs < 0) {
                throw new AiClientException("AI TTS 응답의 재생 시간이 유효하지 않습니다.");
            }
            return new SpeechSynthesisResponse(response.getBody(), durationMs);
        } catch (AiClientException exception) {
            throw exception;
        } catch (NumberFormatException | RestClientException exception) {
            throw new AiClientException("AI 서버와 TTS 통신 중 실패했습니다.", exception);
        }
    }

    private void validatePronunciationResponse(
            PronunciationAnalysisRequest request,
            PronunciationAnalysisResult response
    ) {
        if (!Objects.equals(request.requestId(), response.requestId())) {
            throw new AiClientException("AI 발음 분석 응답의 requestId가 요청과 일치하지 않습니다.");
        }
        int expectedIndex = 0;
        for (PronunciationWordResult word : response.words()) {
            if (word.resultIndex() != expectedIndex++) {
                throw new AiClientException("AI 발음 분석 단어 결과 순서가 올바르지 않습니다.");
            }
        }
    }

    private GenerateStoryResponse requestStory(
            String path,
            Object request,
            String requestId,
            int schemaVersion,
            int currentProgress
    ) {
        try {
            GenerateStoryResponse response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", requestId)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 오류 응답을 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(GenerateStoryResponse.class);

            validateStoryResponse(requestId, schemaVersion, currentProgress, response);
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 통신하는 데 실패했습니다.", exception);
        }
    }

    private void validateResponse(GenerateTrainingRequest request, GenerateTrainingResponse response) {
        if (!Objects.equals(request.requestId(), response.requestId())) {
            throw new AiClientException("AI 서버 응답의 requestId가 요청과 일치하지 않습니다.");
        }
        if (request.schemaVersion() != response.schemaVersion()) {
            throw new AiClientException("AI 서버 응답의 schemaVersion이 요청과 일치하지 않습니다.");
        }
        if (response.generatedData() == null || response.generatedData().isNull()
                || !response.generatedData().isObject()) {
            throw new AiClientException("AI 서버 응답의 generatedData는 JSON 객체여야 합니다.");
        }
    }

    private void validateEvaluationResponse(EvaluateTrainingRequest request, EvaluateTrainingResponse response) {
        if (!Objects.equals(request.requestId(), response.requestId())) {
            throw new AiClientException("AI 훈련 평가 응답의 requestId가 요청과 일치하지 않습니다.");
        }
        if (request.schemaVersion() != response.schemaVersion()) {
            throw new AiClientException("AI 훈련 평가 응답의 schemaVersion이 요청과 일치하지 않습니다.");
        }
        BigDecimal accuracy = response.accuracy();
        if (accuracy == null || accuracy.compareTo(BigDecimal.ZERO) < 0
                || accuracy.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AiClientException("AI 훈련 평가 응답의 accuracy는 0.0 이상 100.0 이하여야 합니다.");
        }
    }

    private void validateStoryResponse(
            String requestId,
            int schemaVersion,
            int currentProgress,
            GenerateStoryResponse response
    ) {
        if (!Objects.equals(requestId, response.requestId())) {
            throw new AiClientException("AI 서버 응답의 requestId가 요청과 일치하지 않습니다.");
        }
        if (schemaVersion != response.schemaVersion()) {
            throw new AiClientException("AI 서버 응답의 schemaVersion이 요청과 일치하지 않습니다.");
        }
        if (response.lines() == null) {
            throw new AiClientException("AI 서버 응답의 lines는 필수입니다.");
        }
        if (response.nextProgress() < currentProgress || response.nextProgress() > 100) {
            throw new AiClientException("AI 서버 응답의 nextProgress가 유효하지 않습니다.");
        }
        if (response.completed() != (response.nextProgress() == 100)) {
            throw new AiClientException("AI 서버 응답의 완료 상태와 진행률이 일치하지 않습니다.");
        }
    }
}
