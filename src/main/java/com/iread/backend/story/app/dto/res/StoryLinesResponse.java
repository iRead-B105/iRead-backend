package com.iread.backend.story.app.dto.res;

import com.iread.backend.story.domain.StoryStatus;
import java.util.List;

public record StoryLinesResponse(
        Long storyId,
        StoryStatus storyStatus,
        int currentDay,
        int availableDay,
        int totalDays,
        int pagesPerDay,
        boolean dayComplete,
        List<StoryLineResponse> storyLines
) {
    public StoryLinesResponse {
        storyLines = List.copyOf(storyLines);
    }
}
