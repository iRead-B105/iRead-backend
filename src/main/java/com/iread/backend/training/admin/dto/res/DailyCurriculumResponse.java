package com.iread.backend.training.admin.dto.res;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;
import java.time.LocalDateTime;

public record DailyCurriculumResponse(
        Long curriculumId,
        String status,
        @JsonSerialize(using = ToStringSerializer.class)
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
