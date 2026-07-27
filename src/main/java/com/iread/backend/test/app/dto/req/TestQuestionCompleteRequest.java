package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record TestQuestionCompleteRequest(
        @NotNull Long testId,
        @NotNull JsonNode result
) {
}
