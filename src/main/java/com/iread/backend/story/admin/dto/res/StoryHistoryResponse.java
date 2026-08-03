package com.iread.backend.story.admin.dto.res;

import com.iread.backend.story.domain.StoryStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record StoryHistoryResponse(
        List<StoryTemplateItem> storyTemplates,
        List<StorySummary> storyHistory,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public StoryHistoryResponse {
        storyTemplates = List.copyOf(storyTemplates);
        storyHistory = List.copyOf(storyHistory);
    }

    public record StoryTemplateItem(
            Long storyTemplateId,
            String storyTemplateTitle,
            String storyTemplateImageUrl
    ) {
    }

    public record StorySummary(
            Long storyId,
            Long storyTemplateId,
            String storyTemplateTitle,
            String storyTemplateImageUrl,
            StoryStatus storyStatus,
            int generationProgress,
            OffsetDateTime createdAt,
            OffsetDateTime lastReadAt,
            OffsetDateTime readingCompletedAt,
            OffsetDateTime activityAt,
            int readLineCount,
            int totalLineCount,
            int readingProgress,
            ReadingStatus readingStatus,
            GazeAnalysisStatus gazeAnalysisStatus
    ) {
    }

    public enum ReadingStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    public enum GazeAnalysisStatus {
        NOT_COLLECTED,
        RUNNING,
        AVAILABLE,
        FAILED
    }
}
