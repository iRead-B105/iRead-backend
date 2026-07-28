package com.iread.backend.pronunciation;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DeterministicPronunciationAnalysisAdapter implements PronunciationAnalysisAdapter {

    public static final String ANALYSIS_VERSION = "PRONUNCIATION_MOCK_V1";

    @Override
    public PronunciationAnalysisResult analyze(PronunciationAnalysisRequest request) {
        String filename = request.originalFilename() == null
                ? "" : request.originalFilename().toLowerCase(Locale.ROOT);
        boolean forceError = filename.contains("error") || filename.contains("fail");
        String errorType = forceError ? "PRONUNCIATION_MISMATCH" : "NONE";
        return new PronunciationAnalysisResult(
                request.requestId(),
                request.expectedText(),
                request.expectedText(),
                forceError ? 54.2 : 95.0,
                forceError ? 0.82 : 0.96,
                errorType,
                ANALYSIS_VERSION
        );
    }
}
