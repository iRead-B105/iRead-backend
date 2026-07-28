package com.iread.backend.test.app.dto.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record TestRecordingRequest(
        @NotNull Long testId,
        @NotNull Long wordId,
        @NotNull MultipartFile audioFile,
        @Min(0) Integer speechStartOffsetMs,
        @Min(0) Integer speechEndOffsetMs
) {
}
