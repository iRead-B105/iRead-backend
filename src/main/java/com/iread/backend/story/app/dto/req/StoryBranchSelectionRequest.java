package com.iread.backend.story.app.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record StoryBranchSelectionRequest(
        @Min(1) @Max(3) Integer optionNo,
        @Size(min = 1, max = 80) String branchIntent
) {
    public StoryBranchSelectionRequest {
        boolean hasOption = optionNo != null;
        boolean hasIntent = branchIntent != null && !branchIntent.isBlank();
        if (hasOption == hasIntent) {
            throw new IllegalArgumentException("optionNo 또는 branchIntent 중 하나만 필요합니다.");
        }
    }

    public StoryBranchSelectionRequest(Integer optionNo) {
        this(optionNo, null);
    }
}
