package com.iread.backend.wordattempt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "word-attempt.score")
public record WordAttemptScoreProperties(
        int retryPenalty,
        int lowPronunciationPenalty,
        int pronunciationThreshold,
        int incorrectPenalty,
        int skippedPenalty,
        int missingAudioPenalty
) {
    public WordAttemptScoreProperties {
        requireNonNegative(retryPenalty, "retry-penalty");
        requireNonNegative(lowPronunciationPenalty, "low-pronunciation-penalty");
        if (pronunciationThreshold < 0 || pronunciationThreshold > 100) {
            throw new IllegalArgumentException(
                    "word-attempt.score.pronunciation-threshold는 0~100이어야 합니다."
            );
        }
        requireNonNegative(incorrectPenalty, "incorrect-penalty");
        requireNonNegative(skippedPenalty, "skipped-penalty");
        requireNonNegative(missingAudioPenalty, "missing-audio-penalty");
    }

    private static void requireNonNegative(int value, String propertyName) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "word-attempt.score." + propertyName + "은 0 이상이어야 합니다."
            );
        }
    }
}
