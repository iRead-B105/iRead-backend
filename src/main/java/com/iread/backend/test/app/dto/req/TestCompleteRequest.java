package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TestCompleteRequest(
        @NotNull Long testId,
        @NotNull LocalDateTime completedAt
) {
}
