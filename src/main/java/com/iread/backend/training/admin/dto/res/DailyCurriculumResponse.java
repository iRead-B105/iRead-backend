package com.iread.backend.training.admin.dto.res;

import java.util.List;

public record DailyCurriculumResponse(
        Long curriculumId,
        String status,
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
