package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;

import java.time.LocalDateTime;

public record StorySessionResponse(
        Long storyId,
        Long studentId,
        Long storyTemplateId,
        LocalDateTime createdAt,
        StoryStatus status
) {
}
