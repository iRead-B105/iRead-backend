package com.iread.backend.ai.dto.res;

import java.util.List;

public record GeneratedStoryBranchPrompt(
        List<GeneratedStoryBranchOption> options
) {
    public GeneratedStoryBranchPrompt {
        options = options == null ? null : List.copyOf(options);
    }
}
