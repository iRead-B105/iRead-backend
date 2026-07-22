package com.iread.backend.story.app.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryChoiceRequest(
        @NotBlank
        @Size(max = 2000)
        String content
) {
}
