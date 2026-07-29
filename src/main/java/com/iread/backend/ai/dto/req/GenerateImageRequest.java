package com.iread.backend.ai.dto.req;

public record GenerateImageRequest(
        String requestId,
        String prompt
) {
}
