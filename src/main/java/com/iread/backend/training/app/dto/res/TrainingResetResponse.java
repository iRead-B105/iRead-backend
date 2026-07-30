package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.TrainingStatus;

import java.time.LocalDateTime;

public record TrainingResetResponse(
        Long trainingId,
        TrainingStatus sessionState,
        LocalDateTime resetAt
) {
}
