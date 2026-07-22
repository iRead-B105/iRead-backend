package com.iread.backend.ai.dto.req;

public record StoryHistoryLine(
        Long storyLineId,
        String content,
        boolean hasChoices,
        String selectedChoice
) {
}
