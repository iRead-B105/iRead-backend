package com.iread.backend.pronunciation;

public record PronunciationWordResult(
        int resultIndex,
        String word,
        Double accuracyScore,
        String errorType,
        int offsetMs,
        int durationMs
) {
    public PronunciationWordResult {
        if (resultIndex < 0) {
            throw new IllegalArgumentException("resultIndex는 0 이상이어야 합니다.");
        }
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("word는 필수입니다.");
        }
        if (accuracyScore != null && (accuracyScore < 0 || accuracyScore > 100)) {
            throw new IllegalArgumentException("accuracyScore는 0 이상 100 이하여야 합니다.");
        }
        if (errorType == null || errorType.isBlank()) {
            throw new IllegalArgumentException("errorType은 필수입니다.");
        }
        if (offsetMs < 0 || durationMs < 0) {
            throw new IllegalArgumentException("음성 구간은 0 이상이어야 합니다.");
        }
    }

    public boolean isInsertion() {
        return "INSERTION".equalsIgnoreCase(errorType);
    }

    public boolean isOmission() {
        return "OMISSION".equalsIgnoreCase(errorType);
    }

    public double scoreOrZero() {
        return accuracyScore == null ? 0 : accuracyScore;
    }
}
