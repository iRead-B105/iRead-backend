package com.iread.backend.story.app.dto.res;

import java.util.List;

public record StorySpeechResponse(
        String transcript,
        double accuracy,
        String readingStatus,
        Double pronunciationAccuracyScore,
        Double fluencyScore,
        Double completenessScore,
        Double pronScore,
        String analysisVersion,
        List<WordResult> words
) {
    public record WordResult(
            Long wordAttemptLogId,
            Integer tokenIndex,
            String expectedText,
            Integer pronunciationAccuracyScore,
            String pronunciationErrorType,
            Integer wordReadTimeMs,
            Boolean isCorrect,
            Integer totalScore,
            List<String> featureCodes
    ) {
    }
}
