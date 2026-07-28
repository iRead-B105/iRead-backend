package com.iread.backend.ai.dto.res;

public record SpeechSynthesisResponse(byte[] audio, long durationMs) {
    public SpeechSynthesisResponse {
        audio = audio.clone();
    }

    @Override
    public byte[] audio() {
        return audio.clone();
    }
}
