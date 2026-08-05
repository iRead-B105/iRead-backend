package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record CompleteTrainingRequest(
        @NotNull JsonNode result,
        @PastOrPresent LocalDateTime completedAt
) {
}
