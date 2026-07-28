package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;
import java.util.List;

public record StoryResumeResponse(
        Long storyId,
        StoryStatus storyStatus,
        List<StoryLineResponse> storyLines
) {
}
