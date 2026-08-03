package com.iread.backend.training.app.dto.res;

import java.time.LocalDate;

public record DemoLearningCheatResponse(
        String action,
        Long curriculumId,
        String curriculumStatus,
        int trainingCount,
        LocalDate currentDate
) {
}
