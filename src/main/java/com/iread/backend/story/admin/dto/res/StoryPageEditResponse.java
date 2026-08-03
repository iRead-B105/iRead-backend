package com.iread.backend.story.admin.dto.res;

import java.util.List;

public record StoryPageEditResponse(
        Long storyLineId,
        Long revision,
        String subtitle,
        String body,
        List<String> choices,
        String imageUrl,
        boolean editable
) {
    public StoryPageEditResponse {
        choices = List.copyOf(choices);
    }
}
