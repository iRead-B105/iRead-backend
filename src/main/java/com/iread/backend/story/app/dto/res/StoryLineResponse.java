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
        /** 이 문장의 다음 장면이 지금 생성되고 있는가. 아동 앱이 선택지를 다시 내주지 않는다. */
        boolean branchGenerating,
        String lineText,
        StoryBranchPromptResponse branchPrompt,
        JsonNode analysis,
        Integer sceneOrder,
        Integer lineOrder,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
