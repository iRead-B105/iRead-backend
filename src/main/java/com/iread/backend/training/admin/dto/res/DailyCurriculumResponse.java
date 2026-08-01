package com.iread.backend.training.admin.dto.res;

import java.util.List;
import java.time.LocalDateTime;

public record DailyCurriculumResponse(
        Long curriculumId,
        String status,
        Long sourceTestCurriculumId,
        String reviewStatus,
        Long reviewedByTeacherId,
        LocalDateTime reviewedAt,
        List<TrainingItem> trainings
) {
    public record TrainingItem(
            Long trainingId,
            Long trainingTemplateId,
            Integer sequence,
            String unitName,
            String trainingName,
            String status
    ) {}
}
