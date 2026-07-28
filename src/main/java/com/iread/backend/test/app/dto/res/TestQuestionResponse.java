package com.iread.backend.test.app.dto.res;

import tools.jackson.databind.JsonNode;

public record TestQuestionResponse(
        Long testId,
        int questionNumber,
        int totalQuestions,
        JsonNode question
) {
}
