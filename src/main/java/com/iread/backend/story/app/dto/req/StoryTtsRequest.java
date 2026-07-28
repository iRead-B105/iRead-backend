package com.iread.backend.story.app.dto.req;

import jakarta.validation.constraints.NotNull;

public record StoryTtsRequest(@NotNull Long lineId) {
}
