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
            Long teacherId,
            Long storyTemplateId,
            LocalDateTime createdAt,
            StoryStatus storyStatus,
            int progress,
            String latestBranchSubtitle,
            String entryImageUrl
    ) {
    }

    public record StoryTemplateItem(
            Long storyTemplateId,
            String templateTitle,
            String imageUrl
    ) {
    }
}
