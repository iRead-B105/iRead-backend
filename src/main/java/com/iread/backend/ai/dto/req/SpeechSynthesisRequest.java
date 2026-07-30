package com.iread.backend.ai.dto.req;

public record SpeechSynthesisRequest(
        String requestId,
        String text,
        String voice
) {
}
