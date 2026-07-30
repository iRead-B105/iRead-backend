package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.TrainingStatus;

import java.time.LocalDateTime;

public record TrainingCompleteResponse(
        String completionType,
        Long trainingId,
        TrainingStatus status,
        LocalDateTime completedAt,
        String messageKey,
        String nextAction
) {
}
