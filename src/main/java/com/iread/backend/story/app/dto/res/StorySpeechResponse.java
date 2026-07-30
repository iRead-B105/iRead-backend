package com.iread.backend.story.app.dto.res;

public record StorySpeechResponse(
        String transcript,
        double accuracy,
        String readingStatus
) {
}
