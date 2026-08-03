package com.iread.backend.story.app.dto.res;

import java.util.List;

public record StoryBranchPromptResponse(
        String subtitle,
        List<Option> options
) {
    public StoryBranchPromptResponse {
        options = List.copyOf(options);
    }

    public record Option(
            int optionNo,
            String label
    ) {
    }
}
