package com.iread.backend.story.app.dto.res;

public record StoryTtsResponse(
        String audioUrl,
        long durationMs,
        Integer playbackLimit
) {
}
