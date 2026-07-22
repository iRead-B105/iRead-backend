package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;

public record StoryResumeResponse(
        Long storyId,
        StoryStatus status,
        StoryLineResponse storyLine
) {
}
