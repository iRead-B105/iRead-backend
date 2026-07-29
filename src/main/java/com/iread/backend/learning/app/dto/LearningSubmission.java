package com.iread.backend.learning.app.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record LearningSubmission(
        @NotNull UUID submissionId,
        @NotNull LearningResponseType responseType,
        @NotNull JsonNode response
) {
}
