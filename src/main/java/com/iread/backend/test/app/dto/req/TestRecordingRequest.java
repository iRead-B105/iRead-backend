package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record TestRecordingRequest(
        @NotNull Long testId,
        Long wordId,
        @Min(0) Integer targetIndex,
        @Min(0) Integer tokenIndex,
        @NotBlank String expectedText,
        @NotNull MultipartFile audioFile,
        @Min(0) Integer speechStartOffsetMs,
        @Min(0) Integer speechEndOffsetMs
) {
    public TestRecordingRequest(
            Long testId,
            Long wordId,
            MultipartFile audioFile,
            Integer speechStartOffsetMs,
            Integer speechEndOffsetMs
    ) {
        this(testId, wordId, null, null, null, audioFile,
                speechStartOffsetMs, speechEndOffsetMs);
    }
}
