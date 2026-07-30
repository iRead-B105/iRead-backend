package com.iread.backend.student.dto.res;

import java.time.LocalDateTime;
import java.util.List;

public record LearningSummaryResponse(
        Long studentId,
        String currentStage,
        LocalDateTime lastLearningAt,
        long attentionRequiredCount,
        List<String> attentionReasons
) {
}
