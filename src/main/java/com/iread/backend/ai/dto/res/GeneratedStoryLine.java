package com.iread.backend.ai.dto.res;

public record GeneratedStoryLine(
        String content,
        boolean requiresBranchInput,
        GeneratedStoryBranchPrompt branchPrompt
) {
    public GeneratedStoryLine(String content, boolean requiresBranchInput) {
        this(content, requiresBranchInput, null);
    }
}
