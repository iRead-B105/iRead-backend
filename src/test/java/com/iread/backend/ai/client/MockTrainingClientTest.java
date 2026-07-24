package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockTrainingClientTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void 훈련_문항_다섯_개를_생성한다() throws Exception {
        GenerateTrainingRequest request = new GenerateTrainingRequest(
                "request-1",
                10L,
                20L,
                30L,
                1,
                objectMapper.readTree("""
                        {
                          "generationSpec": {
                            "questionType": "WORD_GRID_READING",
                            "questionCount": 5
                          },
                          "expectedWords": [{"wordName":"사과"}]
                        }
                        """)
        );

        var response = new MockTrainingGenerator(objectMapper).generate(request);

        assertThat(response.requestId()).isEqualTo("request-1");
        assertThat(response.generatedData().path("questions")).hasSize(5);
        assertThat(response.generatedData().path("questions").get(0).path("problem").path("targetText").asText())
                .isEqualTo("사과");
        assertThat(response.generatedData().path("questions").get(0).path("answer").path("correctText").asText())
                .isEqualTo("사과");
    }

    @Test
    void 정오답_비율로_정확도를_계산한다() throws Exception {
        EvaluateTrainingRequest request = new EvaluateTrainingRequest(
                "evaluation-1",
                10L,
                20L,
                30L,
                1,
                objectMapper.readTree("""
                        {
                          "questions": [
                            {"isCorrect":true},
                            {"isCorrect":true},
                            {"isCorrect":true},
                            {"isCorrect":true},
                            {"isCorrect":false}
                          ]
                        }
                        """)
        );

        var response = new MockTrainingEvaluator().evaluate(request);

        assertThat(response.accuracy()).isEqualByComparingTo(new BigDecimal("80.00"));
    }
}
