package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.NotNull;

public record TestIdRequest(@NotNull Long testId) {
}
