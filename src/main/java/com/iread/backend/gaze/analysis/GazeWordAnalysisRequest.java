package com.iread.backend.gaze.analysis;

public record GazeWordAnalysisRequest(
        String requestId,
        String text,
        Integer gazeStartOffsetMs,
        Integer gazeEndOffsetMs
) {
    public GazeWordAnalysisRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (gazeStartOffsetMs != null && gazeStartOffsetMs < 0
                || gazeEndOffsetMs != null && gazeEndOffsetMs < 0
                || gazeStartOffsetMs != null && gazeEndOffsetMs != null
                && gazeEndOffsetMs < gazeStartOffsetMs) {
            throw new IllegalArgumentException("gaze offsets are invalid");
        }
    }
}
