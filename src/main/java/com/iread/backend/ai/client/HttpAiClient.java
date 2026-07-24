package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class HttpAiClient implements AiClient {

    static final String GENERATE_TRAINING_PATH = "/api/v1/trainings/generate";
    static final String EVALUATE_TRAINING_PATH = "/api/v1/trainings/evaluate";
    static final String GENERATE_STORY_PATH = "/api/v1/story/generate";
    static final String CONTINUE_STORY_PATH = "/api/v1/story/continue";

    private final RestClient restClient;
    private final AiClientProperties properties;
    private final MockTrainingGenerator mockTrainingGenerator;
    private final MockTrainingEvaluator mockTrainingEvaluator;

    public HttpAiClient(
            @Qualifier("aiRestClient") RestClient restClient,
            AiClientProperties properties,
            MockTrainingGenerator mockTrainingGenerator,
            MockTrainingEvaluator mockTrainingEvaluator
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mockTrainingGenerator = mockTrainingGenerator;
        this.mockTrainingEvaluator = mockTrainingEvaluator;
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
        return requestStory(GENERATE_STORY_PATH, request, request.requestId(), request.schemaVersion());
    }

    @Override
    public GenerateStoryResponse continueStory(ContinueStoryRequest request) {
        return requestStory(CONTINUE_STORY_PATH, request, request.requestId(), request.schemaVersion());
    }

    private GenerateStoryResponse requestStory(String path, Object request, String requestId, int schemaVersion) {
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

            validateStoryResponse(requestId, schemaVersion, response);
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

    private void validateStoryResponse(String requestId, int schemaVersion, GenerateStoryResponse response) {
        if (!Objects.equals(requestId, response.requestId())) {
            throw new AiClientException("AI 서버 응답의 requestId가 요청과 일치하지 않습니다.");
        }
        if (schemaVersion != response.schemaVersion()) {
            throw new AiClientException("AI 서버 응답의 schemaVersion이 요청과 일치하지 않습니다.");
        }
        if (response.lines() == null) {
            throw new AiClientException("AI 서버 응답의 lines는 필수입니다.");
        }
    }
}
