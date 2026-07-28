package com.iread.backend.ai.dto.res;

public record SpeechTranscriptionResponse(
        String requestId,
        String transcript,
        double confidence,
        long durationMs
) {
}
