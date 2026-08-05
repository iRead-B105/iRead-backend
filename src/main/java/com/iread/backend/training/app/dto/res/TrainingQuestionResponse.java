package com.iread.backend.training.app.dto.res;

import tools.jackson.databind.JsonNode;

public record TrainingQuestionResponse(
        Long trainingId,
        Integer questionNumber,
        Integer totalQuestions,
        JsonNode question
) {
}
