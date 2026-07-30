package com.iread.backend.pronunciation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeterministicPronunciationAnalysisAdapter implements PronunciationAnalysisAdapter {

    public static final String ANALYSIS_VERSION = "PRONUNCIATION_MOCK_V1";
    private static final Pattern WORD_PATTERN = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+");

    @Override
    public PronunciationAnalysisResult analyze(PronunciationAnalysisRequest request) {
        String filename = request.originalFilename() == null
                ? "" : request.originalFilename().toLowerCase(Locale.ROOT);
        boolean forceError = filename.contains("error") || filename.contains("fail");
        double score = forceError ? 54.2 : 95.0;
        String errorType = forceError ? "Mispronunciation" : "None";
        List<PronunciationWordResult> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(request.expectedText());
        int index = 0;
        while (matcher.find()) {
            words.add(new PronunciationWordResult(
                    index,
                    matcher.group(),
                    score,
                    errorType,
                    index * 500,
                    400
            ));
            index++;
        }
        return new PronunciationAnalysisResult(
                request.requestId(),
                score,
                forceError ? 61.0 : 94.0,
                100.0,
                forceError ? 57.0 : 95.0,
                forceError ? 0.82 : 0.96,
                ANALYSIS_VERSION,
                words
        );
    }
}
