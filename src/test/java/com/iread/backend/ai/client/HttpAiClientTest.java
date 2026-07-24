package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.ContinueStoryRequest;
import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateStoryRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.req.StoryHistoryLine;
import com.iread.backend.ai.dto.req.StoryTemplateData;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.ai.config.AiClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.net.URI;
import java.time.Duration;

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
        aiClient = new HttpAiClient(
                builder.baseUrl("http://localhost:8081").build(),
                new AiClientProperties(
                        URI.create("http://localhost:8081"),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        "",
                        false,
                        false
                ),
                new MockTrainingGenerator(objectMapper),
                new MockTrainingEvaluator()
        );
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
                            {"content": "숲에 도착했어요.", "requiresBranchInput": false},
                            {"content": "어디로 갈까요?", "requiresBranchInput": true}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = aiClient.generateStory(request);

        assertThat(response.completed()).isFalse();
        assertThat(response.lines()).extracting("content")
                .containsExactly("숲에 도착했어요.", "어디로 갈까요?");
        assertThat(response.lines().getLast().requiresBranchInput()).isTrue();
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
}
