package com.iread.backend.pronunciation;

public record PronunciationReferenceWord(
        Integer tokenIndex,
        String surface
) {
    public PronunciationReferenceWord {
        if (tokenIndex != null && tokenIndex < 0) {
            throw new IllegalArgumentException("tokenIndex는 0 이상이어야 합니다.");
        }
        if (surface == null || surface.isBlank()) {
            throw new IllegalArgumentException("surface는 필수입니다.");
        }
    }
}
