package com.iread.backend.ai.dto.res;

public record GenerateImageResponse(
        String requestId,
        String imageUrl,
        String provider
) {
}
