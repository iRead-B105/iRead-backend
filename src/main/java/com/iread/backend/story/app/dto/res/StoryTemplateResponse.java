package com.iread.backend.story.app.dto.res;

public record StoryTemplateResponse(
        Long storyTemplateId,
        String templateTitle,
        String templateContent
) {
}
