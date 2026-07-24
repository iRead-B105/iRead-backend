package com.iread.backend.training.admin.dto.res;

import java.util.List;

public record DailyCurriculumResponse(Long curriculumId, List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, Long trainingTemplateId, String unitName, String trainingName) {}
}
