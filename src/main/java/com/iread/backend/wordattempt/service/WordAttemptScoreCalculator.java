package com.iread.backend.wordattempt.service;

import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WordAttemptScoreCalculator {

    public static final int INITIAL_SCORE = 1000;
    public static final int MINIMUM_SCORE = 0;

    private final WordAttemptScoreProperties properties;

    public int calculate(
            Integer pronunciationAccuracyScore,
            boolean hasAudioData,
            Boolean skipped,
            Integer retryCount,
            Boolean correct
    ) {
        long deduction = 0;
        deduction += (long) Math.max(retryCount == null ? 0 : retryCount, 0)
                * properties.retryPenalty();

        if (Boolean.TRUE.equals(skipped)) {
            deduction += properties.skippedPenalty();
        }
        if (Boolean.FALSE.equals(correct)) {
            deduction += properties.incorrectPenalty();
        }

        if (!hasAudioData || pronunciationAccuracyScore == null) {
            deduction += properties.missingAudioPenalty();
        } else if (!meetsPronunciationThreshold(pronunciationAccuracyScore)) {
            deduction += properties.lowPronunciationPenalty();
        }

        return (int) Math.max(MINIMUM_SCORE, INITIAL_SCORE - deduction);
    }

    public boolean meetsPronunciationThreshold(int scaledAccuracyScore) {
        return scaledAccuracyScore >= properties.pronunciationThreshold() * 10;
    }

    public boolean meetsPronunciationThreshold(double accuracyScore) {
        return accuracyScore >= properties.pronunciationThreshold();
    }

    public int pronunciationThreshold() {
        return properties.pronunciationThreshold();
    }
}
