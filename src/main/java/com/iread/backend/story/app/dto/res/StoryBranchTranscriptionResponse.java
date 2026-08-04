package com.iread.backend.story.app.dto.res;

public record StoryBranchTranscriptionResponse(
        String transcript,
        double confidence,
        String decision,
        String reasonCode,
        String policyVersion,
        String reviewToken
) {
}
