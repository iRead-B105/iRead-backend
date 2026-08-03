package com.iread.backend.story.admin.dto.req;

import jakarta.validation.constraints.NotNull;

public record StoryPageImageRegenerateRequest(@NotNull Long revision) {
}
