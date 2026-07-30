package com.iread.backend.training.app.dto.res;

import com.iread.backend.learning.app.dto.LearningErrorLocation;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public record TrainingFeedbackResponse(
        String feedbackType,
        UUID submissionId,
        int attemptNo,
        int maxAttempts,
        int remainingAttempts,
        boolean correct,
        boolean questionCompleted,
        boolean canRetry,
        List<LearningErrorLocation> errorLocations,
        String hint,
        JsonNode correctResponse
) {
}
