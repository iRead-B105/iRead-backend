package com.iread.backend.ai.dto.req;

import java.util.Objects;

public record GenerateStoryRequest(
        String requestId,
        Long storyId,
        Long studentId,
        int schemaVersion,
        StoryTemplateData storyTemplate
) {
    public GenerateStoryRequest {
        requireText(requestId, "requestId");
        requirePositive(storyId, "storyId");
        requirePositive(studentId, "studentId");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion은 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(storyTemplate, "storyTemplate은 필수입니다.");
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(fieldName + "는 1 이상이어야 합니다.");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }
}
