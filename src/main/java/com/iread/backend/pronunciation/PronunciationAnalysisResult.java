package com.iread.backend.pronunciation;

import java.util.List;

public record PronunciationAnalysisResult(
        String requestId,
        double pronunciationAccuracyScore,
        Double fluencyScore,
        Double completenessScore,
        Double pronScore,
        double confidence,
        String analysisVersion,
        List<PronunciationWordResult> words
) {
    public PronunciationAnalysisResult {
        requireScore(pronunciationAccuracyScore, "pronunciationAccuracyScore");
        requireNullableScore(fluencyScore, "fluencyScore");
        requireNullableScore(completenessScore, "completenessScore");
        requireNullableScore(pronScore, "pronScore");
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId는 필수입니다.");
        }
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence는 0 이상 1 이하여야 합니다.");
        }
        if (analysisVersion == null || analysisVersion.isBlank()) {
            throw new IllegalArgumentException("analysisVersion은 필수입니다.");
        }
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("단어별 발음 분석 결과가 필요합니다.");
        }
        words = List.copyOf(words);
    }

    private static void requireNullableScore(Double score, String name) {
        if (score != null) {
            requireScore(score, name);
        }
    }

    private static void requireScore(double score, String name) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(name + "는 0 이상 100 이하여야 합니다.");
        }
    }
}
