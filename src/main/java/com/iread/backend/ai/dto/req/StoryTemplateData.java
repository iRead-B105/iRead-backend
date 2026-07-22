package com.iread.backend.ai.dto.req;

public record StoryTemplateData(
        Long storyTemplateId,
        String title,
        String context
) {
}
