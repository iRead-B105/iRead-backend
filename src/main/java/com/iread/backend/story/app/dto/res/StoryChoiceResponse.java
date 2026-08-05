package com.iread.backend.story.app.dto.res;

public record StoryChoiceResponse(
        Long choiceId,
        String transcript,
        Long nextSceneId,
        Long nextLineId,
        String generatedContent,
        String imageUrl,
        int progress,
        String status,
        boolean replayed
) {
}
