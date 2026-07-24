package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;

import java.util.List;

public record StoryChoiceResponse(
        Long storyLineId,
        StoryStatus status,
        List<StoryLineResponse> generatedLines
) {
}
