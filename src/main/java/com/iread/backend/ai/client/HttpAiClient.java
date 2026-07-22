package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import com.iread.backend.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Component
public class HttpAiClient implements AiClient {

    static final String GENERATE_TRAINING_PATH = "/api/v1/trainings/generate";

    private final RestClient restClient;

    public HttpAiClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public GenerateTrainingResponse generateTraining(GenerateTrainingRequest request) {
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
}
