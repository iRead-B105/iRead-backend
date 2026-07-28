package com.iread.backend.training.app.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainingRecordingRequest(
        @NotNull Long wordId,
        @NotBlank String recognizedText,
        @Min(0) Integer speechStartOffsetMs,
        @Min(0) Integer speechEndOffsetMs,
        Boolean isCorrect,
        @NotNull @Min(0) @Max(1000) Integer totalScore
) {
}
