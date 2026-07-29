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

    public Integer calculate(
            Integer pronunciationAccuracyScore,
            boolean pronunciationRequired,
            boolean hasAudioData,
            Boolean pronunciationSkipped,
            boolean gazeRequired,
            boolean hasGazeData,
            Boolean gazeSkipped,
            Integer regressionCount,
            Integer retryCount,
            Boolean correct
    ) {
        if (pronunciationRequired
                && (!hasAudioData || pronunciationAccuracyScore == null)) {
            return null;
        }
        if (gazeRequired && !hasGazeData) {
            return null;
        }

        long activeWeight = properties.taskWeight();
        long weightedScore = (long) taskScore(
                pronunciationSkipped,
                gazeSkipped,
                retryCount,
                correct
        ) * properties.taskWeight();

        if (pronunciationRequired) {
            activeWeight += properties.pronunciationWeight();
            weightedScore += (long) clamp(pronunciationAccuracyScore)
                    * properties.pronunciationWeight();
        }
        if (gazeRequired) {
            activeWeight += properties.gazeWeight();
            weightedScore += (long) gazeScore(gazeSkipped, regressionCount)
                    * properties.gazeWeight();
        }

        if (activeWeight == 0) {
            throw new IllegalStateException(
                    "현재 문항에 적용할 word-attempt.score 가중치가 없습니다."
            );
        }
        return clamp((int) Math.round((double) weightedScore / activeWeight));
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

    private int taskScore(
            Boolean pronunciationSkipped,
            Boolean gazeSkipped,
            Integer retryCount,
            Boolean correct
    ) {
        long deduction = (long) Math.max(retryCount == null ? 0 : retryCount, 0)
                * properties.retryPenalty();
        if (Boolean.TRUE.equals(pronunciationSkipped)
                || Boolean.TRUE.equals(gazeSkipped)) {
            deduction += properties.skippedPenalty();
        }
        if (Boolean.FALSE.equals(correct)) {
            deduction += properties.incorrectPenalty();
        }
        return clamp((int) Math.max(MINIMUM_SCORE, INITIAL_SCORE - deduction));
    }

    private int gazeScore(Boolean gazeSkipped, Integer regressionCount) {
        if (Boolean.TRUE.equals(gazeSkipped)) {
            return MINIMUM_SCORE;
        }
        long deduction = (long) Math.max(
                regressionCount == null ? 0 : regressionCount,
                0
        ) * properties.gazeRegressionPenalty();
        return clamp((int) Math.max(MINIMUM_SCORE, INITIAL_SCORE - deduction));
    }

    private int clamp(Integer score) {
        if (score == null) {
            return MINIMUM_SCORE;
        }
        return Math.max(MINIMUM_SCORE, Math.min(INITIAL_SCORE, score));
    }
}
