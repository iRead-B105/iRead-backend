package com.iread.backend.training.generation;

import com.iread.backend.ai.exception.AiClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "ai.mock-generate", havingValue = "false")
public class HttpTrainingCandidateProvider implements TrainingCandidateProvider {

    static final String GENERATE_CANDIDATES_PATH = "/api/v1/trainings/candidates";

    private final RestClient restClient;

    public HttpTrainingCandidateProvider(
            @Qualifier("aiRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Override
    public TrainingCandidateResponse generate(TrainingCandidateRequest request) {
        try {
            TrainingCandidateResponse response = restClient.post()
                    .uri(GENERATE_CANDIDATES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", request.requestId())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, httpResponse) -> {
                        throw new AiClientException(
                                "AI 서버가 훈련 문항 후보 생성 오류를 반환했습니다.",
                                httpResponse.getStatusCode().value()
                        );
                    })
                    .requiredBody(TrainingCandidateResponse.class);
            if (!request.trainingType().name().equals(response.type())) {
                throw new AiClientException(
                        "AI 훈련 문항 후보 응답의 type이 요청과 일치하지 않습니다."
                );
            }
            return response;
        } catch (AiClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiClientException(
                    "AI 서버와 훈련 문항 후보 생성 통신 중 실패했습니다.",
                    exception
            );
        }
    }
}
