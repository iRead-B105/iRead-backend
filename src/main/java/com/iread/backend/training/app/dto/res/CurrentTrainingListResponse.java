package com.iread.backend.training.app.dto.res;

import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingStatus;
import com.iread.backend.training.generation.TrainingType;

import java.util.List;

public record CurrentTrainingListResponse(
        Long curriculumId,
        DailyCurriculumStatus curriculumStatus,
        List<TrainingItem> trainings
) {
    public CurrentTrainingListResponse {
        trainings = List.copyOf(trainings);
    }

    public record TrainingItem(
            Long trainingId,
            Long trainingTemplateId,
            TrainingType trainingType,
            Integer sequenceNo,
            String unitName,
            String trainingName,
            TrainingStatus status
    ) {
    }
}
