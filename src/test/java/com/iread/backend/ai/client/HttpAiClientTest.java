package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.exception.AiClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

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

    private MockRestServiceServer server;
    private HttpAiClient aiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        aiClient = new HttpAiClient(builder.baseUrl("http://localhost:8081").build());
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
}
