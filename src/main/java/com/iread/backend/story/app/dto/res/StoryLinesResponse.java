package com.iread.backend.story.app.dto.res;

import java.util.List;

public record StoryLinesResponse(List<StoryLineResponse> storyLines) {
    public StoryLinesResponse {
        storyLines = List.copyOf(storyLines);
    }
}
