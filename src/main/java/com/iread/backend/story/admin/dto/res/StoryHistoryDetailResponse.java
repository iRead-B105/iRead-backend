package com.iread.backend.story.admin.dto.res;

import java.time.OffsetDateTime;
import java.util.List;

public record StoryHistoryDetailResponse(
        StoryHistoryResponse.StorySummary story,
        List<StoryPage> pages,
        int totalPages
) {
    public StoryHistoryDetailResponse {
        pages = List.copyOf(pages);
    }

    public record StoryPage(
            int pageNo,
            Long storyLineId,
            Long sceneId,
            int sceneOrder,
            int lineOrder,
            String backgroundImageUrl,
            String backgroundImagePosition,
            ImageGenerationStatus imageGenerationStatus,
            List<String> textLines,
            boolean requiresBranchInput,
            OffsetDateTime readAt,
            BranchRecord branchRecord,
            long revision,
            boolean editable,
            String subtitle,
            List<String> choices
    ) {
        public StoryPage {
            textLines = List.copyOf(textLines);
            choices = List.copyOf(choices);
        }
    }

    public record BranchRecord(
            Long choiceId,
            String promptText,
            String transcript,
            OffsetDateTime createdAt
    ) {
    }

    public enum ImageGenerationStatus {
        NOT_REQUESTED,
        PENDING,
        AVAILABLE,
        FAILED
    }
}
