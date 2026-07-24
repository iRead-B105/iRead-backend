package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.EvaluateTrainingRequest;
import com.iread.backend.ai.dto.res.EvaluateTrainingResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MockTrainingEvaluator {

    public EvaluateTrainingResponse evaluate(EvaluateTrainingRequest request) {
        JsonNode questions = request.result().path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            return response(request, BigDecimal.ZERO.setScale(2));
        }

        long correctCount = 0;
        for (JsonNode question : questions) {
            if (question.path("isCorrect").asBoolean(false)) {
                correctCount++;
            }
        }
        BigDecimal accuracy = BigDecimal.valueOf(correctCount * 100L)
                .divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP);
        return response(request, accuracy);
    }

    private EvaluateTrainingResponse response(EvaluateTrainingRequest request, BigDecimal accuracy) {
        return new EvaluateTrainingResponse(request.requestId(), request.schemaVersion(), accuracy);
    }
}
