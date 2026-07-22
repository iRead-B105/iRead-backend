package com.iread.backend.ai.dto.req;

import java.util.List;
import java.util.Objects;

public record ContinueStoryRequest(
        String requestId,
        Long storyId,
        Long studentId,
        int schemaVersion,
        StoryTemplateData storyTemplate,
        Long selectedStoryLineId,
        String choice,
        List<StoryHistoryLine> history
) {
    public ContinueStoryRequest {
        requireText(requestId, "requestId");
        requirePositive(storyId, "storyId");
        requirePositive(studentId, "studentId");
        requirePositive(selectedStoryLineId, "selectedStoryLineId");
        requireText(choice, "choice");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion은 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(storyTemplate, "storyTemplate은 필수입니다.");
        history = List.copyOf(Objects.requireNonNull(history, "history는 필수입니다."));
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
