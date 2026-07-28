package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

@Component
public class HttpAiClient implements AiClient {

    static final String GENERATE_TRAINING_PATH = "/api/v1/trainings/generate";
    static final String EVALUATE_TRAINING_PATH = "/api/v1/trainings/evaluate";
    static final String GENERATE_STORY_PATH = "/api/v1/story/generate";
    static final String CONTINUE_STORY_PATH = "/api/v1/story/continue";
    static final String TRANSCRIBE_SPEECH_PATH = "/api/v1/speech/transcribe";
    static final String SYNTHESIZE_SPEECH_PATH = "/api/v1/speech/synthesize";

    private final RestClient restClient;
    private final AiClientProperties properties;
    private final MockTrainingGenerator mockTrainingGenerator;
    private final MockTrainingEvaluator mockTrainingEvaluator;
    private final MockStoryGenerator mockStoryGenerator;
    private final MockSpeechProcessor mockSpeechProcessor;

    public HttpAiClient(
            @Qualifier("aiRestClient") RestClient restClient,
            AiClientProperties properties,
            MockTrainingGenerator mockTrainingGenerator,
            MockTrainingEvaluator mockTrainingEvaluator,
            MockStoryGenerator mockStoryGenerator,
            MockSpeechProcessor mockSpeechProcessor
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mockTrainingGenerator = mockTrainingGenerator;
        this.mockTrainingEvaluator = mockTrainingEvaluator;
        this.mockStoryGenerator = mockStoryGenerator;
        this.mockSpeechProcessor = mockSpeechProcessor;
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
    public SpeechTranscriptionResponse transcribeSpeech(
            String requestId, Long studentId, String expectedText, MultipartFile audioFile
    ) {
        if (properties.mockSpeech()) {
            return mockSpeechProcessor.transcribe(requestId, expectedText);
        }
        try {
            MultipartBodyBuilder body = new MultipartBodyBuilder();
            body.part("requestId", requestId);
            body.part("studentId", studentId.toString());
            if (expectedText != null && !expectedText.isBlank()) {
                body.part("expectedText", expectedText);
            }
            byte[] audio = audioFile.getBytes();
            body.part("audioFile", new ByteArrayResource(audio) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            }).contentType(MediaType.parseMediaType(
                    Objects.requireNonNullElse(audioFile.getContentType(), "application/octet-stream")
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
        } catch (IOException | RestClientException exception) {
            throw new AiClientException("AI 서버와 음성 인식 통신 중 실패했습니다.", exception);
        }
    }

    @Override
    public SpeechSynthesisResponse synthesizeSpeech(SpeechSynthesisRequest request) {
        if (properties.mockSpeech()) {
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
