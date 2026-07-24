package com.iread.backend.training.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CompleteTrainingRequest(
        @NotNull JsonNode result
) {
}
