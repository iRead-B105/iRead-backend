package com.iread.backend.ai.dto.res;

import java.util.List;

public record GeneratedStoryBranchPrompt(
        String subtitle,
        List<GeneratedStoryBranchOption> options
) {
    public GeneratedStoryBranchPrompt {
        options = options == null ? null : List.copyOf(options);
        if ((subtitle == null || subtitle.isBlank()) && options != null && !options.isEmpty()) {
            subtitle = options.getFirst().label();
        }
    }

    public GeneratedStoryBranchPrompt(List<GeneratedStoryBranchOption> options) {
        this(null, options);
    }
}
