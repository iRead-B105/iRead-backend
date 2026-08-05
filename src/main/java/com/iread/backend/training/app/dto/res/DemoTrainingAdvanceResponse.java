package com.iread.backend.training.app.dto.res;

import java.time.LocalDate;

public record DemoTrainingAdvanceResponse(
        String action,
        Long curriculumId,
        String curriculumStatus,
        Long completedTrainingId,
        String completedTrainingStatus,
        Long nextTrainingId,
        String nextTrainingStatus,
        LocalDate currentDate
) {
}
