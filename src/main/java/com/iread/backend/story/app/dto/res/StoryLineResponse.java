package com.iread.backend.story.app.dto.res;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record StoryLineResponse(
        Long lineId,
        Long previousLineId,
        Long sceneId,
        Long storyId,
        String imageUrl,
        boolean requiresBranchInput,
        String lineText,
        JsonNode analysis,
        Integer sceneOrder,
        Integer lineOrder,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
