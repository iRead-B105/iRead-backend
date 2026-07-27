package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TestSelectionRequest(
        @NotNull Long testId,
        @NotNull Long wordId,
        @NotNull Boolean isCorrect,
        @NotNull @Min(0) @Max(1000) Integer totalScore
) {
}
