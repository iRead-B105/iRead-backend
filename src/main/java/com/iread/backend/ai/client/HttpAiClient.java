package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.req.StoryBranchInputReviewRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateImageResponse;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.ai.dto.res.StoryBranchInputReviewResponse;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.demo.DemoStoryReplayer;
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
import java.util.Objects;

@Component
public class HttpAiClient implements AiClient {

    private static final MediaType TEXT_PLAIN_UTF8 =
            new MediaType("text", "plain", StandardCharsets.UTF_8);
    private static final String MOCK_PNG_DATA_URL =
            "data:image/png;base64,"
                    + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    static final String GENERATE_TRAINING_PATH = "/api/v1/trainings/generate";
    static final String EVALUATE_TRAINING_PATH = "/api/v1/trainings/evaluate";
    static final String GENERATE_STORY_PATH = "/api/v1/story/generate";
    static final String CONTINUE_STORY_PATH = "/api/v1/story/continue";
    static final String REVIEW_STORY_BRANCH_INPUT_PATH = "/api/v1/story/branch-input/review";
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
    private final DemoStoryReplayer demoStoryReplayer;
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
            FileStorage fileStorage,
            DemoStoryReplayer demoStoryReplayer
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mockTrainingGenerator = mockTrainingGenerator;
        this.mockTrainingEvaluator = mockTrainingEvaluator;
        this.mockStoryGenerator = mockStoryGenerator;
        this.mockSpeechProcessor = mockSpeechProcessor;
        this.temporaryAudioStorage = temporaryAudioStorage;
        this.fileStorage = fileStorage;
        this.demoStoryReplayer = demoStoryReplayer;
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
        // 시연 토글이 켜져 있으면 mock/실연동 여부와 무관하게 사전 제작 스토리를 우선한다
        var replayed = demoStoryReplayer.replayGenerate(request);
        if (replayed.isPresent()) {
            return replayed.get();
        }
        if (properties.storyMocked()) {
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
        // 합의된 선택 경로를 벗어나면 replayContinue가 비어 실제 생성으로 폴백된다
        var replayed = demoStoryReplayer.replayContinue(request);
        if (replayed.isPresent()) {
            return replayed.get();
        }
        if (properties.storyMocked()) {
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
    public StoryBranchInputReviewResponse reviewStoryBranchInput(
            StoryBranchInputReviewRequest request
    ) {
        if (properties.storyMocked()) {
            return mockStoryBranchInputReview(request);
        }
        try {
            StoryBranchInputReviewResponse response = restClient.post()
                    .uri(REVIEW_STORY_BRANCH_INPUT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 분기 입력 검토 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(StoryBranchInputReviewResponse.class);
            if (!Objects.equals(request.requestId(), response.requestId())
                    || response.decision() == null
                    || response.reasonCode() == null
                    || response.policyVersion() == null
                    || response.policyVersion().isBlank()) {
                throw new AiClientException("AI 분기 입력 검토 응답 값이 유효하지 않습니다.");
            }
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException("AI 서버와 분기 입력 검토 통신 중 실패했습니다.", exception);
        }
    }

    private StoryBranchInputReviewResponse mockStoryBranchInputReview(
            StoryBranchInputReviewRequest request
    ) {
        String transcript = Objects.toString(request.transcript(), "");
        StoryBranchInputReviewResponse.ReasonCode reason =
                transcript.matches(".*(자살|죽고 싶|목을 잘라|성폭행|진짜 죽일|시스템 프롬프트|이전 지시를 무시).*")
                        ? StoryBranchInputReviewResponse.ReasonCode.SELF_HARM
                        : StoryBranchInputReviewResponse.ReasonCode.OK;
        StoryBranchInputReviewResponse.Decision decision =
                reason == StoryBranchInputReviewResponse.ReasonCode.OK
                        ? StoryBranchInputReviewResponse.Decision.ALLOW
                        : StoryBranchInputReviewResponse.Decision.BLOCK;
        return new StoryBranchInputReviewResponse(
                request.requestId(), decision, reason, "story-branch-input-v1"
        );
    }

    @Override
    public GenerateImageResponse generateImage(GenerateImageRequest request) {
        // 사전 제작 삽화는 이미 uploads에 설치되어 있어 재저장 없이 URL만 돌려준다
        var replayedImage = demoStoryReplayer.replayImage(request);
        if (replayedImage.isPresent()) {
            return replayedImage.get();
        }
        if (properties.imageMocked()) {
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
        boolean storyCharacter = prompt.startsWith("[STORY_CHARACTER]");
        return new GenerateImageResponse(
                request.requestId(),
                MOCK_PNG_DATA_URL,
                storyCharacter
                        ? "BACKEND_MOCK_STORY_CHARACTER_PNG_V1"
                        : "BACKEND_MOCK_IMAGE_PNG_V1"
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
            body.part("requestId", requestId).contentType(TEXT_PLAIN_UTF8);
            body.part("studentId", studentId.toString()).contentType(TEXT_PLAIN_UTF8);
            if (expectedText != null && !expectedText.isBlank()) {
                body.part("expectedText", expectedText).contentType(TEXT_PLAIN_UTF8);
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
            body.part("requestId", request.requestId()).contentType(TEXT_PLAIN_UTF8);
            body.part("expectedText", request.expectedText()).contentType(TEXT_PLAIN_UTF8);
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
