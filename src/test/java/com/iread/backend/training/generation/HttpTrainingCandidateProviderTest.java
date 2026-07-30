package com.iread.backend.training.generation;

import com.iread.backend.ai.exception.AiClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpTrainingCandidateProviderTest {

    private final JsonMapper objectMapper = new JsonMapper();
    private MockRestServiceServer server;
    private HttpTrainingCandidateProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new HttpTrainingCandidateProvider(
                builder.baseUrl("http://localhost:8081").build()
        );
    }

    @Test
    void sendsCandidateContractToMockAiServer() {
        TrainingCandidateRequest request = request("candidate-request-1");
        server.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/trainings/candidates"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "candidate-request-1"))
                .andRespond(withSuccess(
                        """
                        {
                          "type": "CONSONANT_SOUND_CHOICE",
                          "data": [
                            {"audioText":"ㄱ","choices":["ㄱ","ㄴ","ㄷ"],"answerIndex":0}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        TrainingCandidateResponse response = provider.generate(request);

        assertThat(response.type()).isEqualTo("CONSONANT_SOUND_CHOICE");
        assertThat(response.data()).hasSize(1);
        server.verify();
    }

    @Test
    void rejectsMismatchedTrainingType() {
        server.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/trainings/candidates"
                ))
                .andRespond(withSuccess(
                        """
                        {"type":"VOWEL_TRACE","data":[]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> provider.generate(request("candidate-request-2")))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("type");
    }

    @Test
    void wrapsMockAiServerFailure() {
        server.expect(once(), requestTo(
                        "http://localhost:8081/api/v1/trainings/candidates"
                ))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.generate(request("candidate-request-3")))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("후보 생성 오류");
    }

    private TrainingCandidateRequest request(String requestId) {
        ObjectNode outputTemplate = objectMapper.createObjectNode();
        outputTemplate.put("audioText", "<string>");
        outputTemplate.putArray("choices").add("<string>");

        return new TrainingCandidateRequest(
                requestId,
                2,
                TrainingType.CONSONANT_SOUND_CHOICE,
                5,
                2,
                List.of(),
                List.of(),
                "정답 1개와 오답 2개를 생성한다.",
                outputTemplate
        );
    }
}
