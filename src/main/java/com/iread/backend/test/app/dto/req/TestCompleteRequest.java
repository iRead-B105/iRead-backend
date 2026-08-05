package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.NotNull;

public record TestCompleteRequest(
        @NotNull Long testId
) {
}
