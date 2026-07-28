package com.iread.backend.gaze.analysis;

import org.springframework.stereotype.Component;

@Component
public class DeterministicGazeWordAnalysisAdapter implements GazeWordAnalysisAdapter {

    public static final String ANALYSIS_VERSION = "GAZE_MOCK_V1";

    @Override
    public GazeWordAnalysisResult analyze(GazeWordAnalysisRequest request) {
        int duration = request.gazeStartOffsetMs() != null && request.gazeEndOffsetMs() != null
                ? request.gazeEndOffsetMs() - request.gazeStartOffsetMs()
                : 500 + request.text().codePointCount(0, request.text().length()) * 180;
        boolean skipped = duration == 0;
        int fixationCount = skipped ? 0 : Math.max(1, (int) Math.ceil(duration / 500.0));
        int regressionCount = duration >= 1_500 ? 2 : duration >= 1_000 ? 1 : 0;
        return new GazeWordAnalysisResult(
                request.requestId(),
                duration,
                fixationCount,
                regressionCount,
                skipped,
                0.90,
                ANALYSIS_VERSION
        );
    }
}
