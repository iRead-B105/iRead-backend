package com.iread.backend.training.app.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record TrainingRecordingRequest(
        @NotNull Long wordId,
        @NotNull @Min(0) Integer targetIndex,
        @Min(0) Integer tokenIndex,
        @NotBlank String expectedText,
        @NotBlank String expectedPronunciation,
        @NotNull MultipartFile audioFile,
        @Min(0) Integer speechStartOffsetMs,
        @Min(0) Integer speechEndOffsetMs
) {
}
