package com.iread.backend.wordattempt.service;

import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WordAttemptScoreCalculator {

    public static final int INITIAL_SCORE = 1000;
    public static final int MINIMUM_SCORE = 0;

    private final WordAttemptScoreProperties properties;

    public int calculate(
            String surfaceText,
            String recognizedText,
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

        if (!hasAudioData || recognizedText == null || recognizedText.isBlank()) {
            deduction += properties.missingAudioPenalty();
        } else if (!normalize(surfaceText).equals(normalize(recognizedText))) {
            deduction += properties.sttMismatchPenalty();
        }

        return (int) Math.max(MINIMUM_SCORE, INITIAL_SCORE - deduction);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{P}]", "")
                .toLowerCase(Locale.ROOT);
    }
}
