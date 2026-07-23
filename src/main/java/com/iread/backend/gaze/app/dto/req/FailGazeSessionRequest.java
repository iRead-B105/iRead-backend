package com.iread.backend.gaze.app.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FailGazeSessionRequest(
        @NotNull
        @Schema(description = "학생 ID", example = "1")
        Long studentId
) {
}