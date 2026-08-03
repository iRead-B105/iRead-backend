package com.iread.backend.story.app.dto.res;

public record StoryBranchTranscriptionResponse(
        String transcript,
        double confidence,
        boolean accepted
) {
}
