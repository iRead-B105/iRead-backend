package com.iread.backend.pronunciation;

public record PronunciationAnalysisRequest(
        String requestId,
        String expectedText,
        String originalFilename,
        byte[] audio
) {
    public PronunciationAnalysisRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId는 필수입니다.");
        }
        if (expectedText == null || expectedText.isBlank()) {
            throw new IllegalArgumentException("expectedText는 필수입니다.");
        }
        if (audio == null || audio.length == 0) {
            throw new IllegalArgumentException("audio는 비어 있을 수 없습니다.");
        }
        audio = audio.clone();
    }
}
