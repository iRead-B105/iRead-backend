package com.iread.backend.story.app.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StoryBranchSelectionRequest(
        @NotNull @Min(1) @Max(3) Integer optionNo
) {
}
