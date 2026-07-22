package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;

import java.time.LocalDateTime;
import java.util.List;

public record StoryShelfResponse(
        List<StoryItem> stories,
        List<StoryTemplateItem> storyTemplates
) {
    public record StoryItem(
            Long storyId,
            Long studentId,
            Long storyTemplateId,
            LocalDateTime createdAt,
            StoryStatus status
    ) {
    }

    public record StoryTemplateItem(
            Long storyTemplateId,
            String title
    ) {
    }
}
