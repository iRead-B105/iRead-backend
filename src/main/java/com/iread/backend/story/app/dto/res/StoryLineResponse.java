package com.iread.backend.story.app.dto.res;

import java.time.LocalDateTime;

public record StoryLineResponse(
        Long storyLineId,
        Long previousStoryLineId,
        Long storyId,
        String imageUrl,
        boolean requiresBranchInput,
        String content,
        Integer sequenceNo,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
