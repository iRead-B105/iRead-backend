package com.iread.backend.story.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StoryPageUpdateRequest(
        @NotNull Long revision,
        @Size(min = 1, max = 40) String subtitle,
        @Size(min = 1, max = 500) String body,
        @Size(min = 3, max = 3) List<@Size(min = 1, max = 40) String> choices
) {
}
