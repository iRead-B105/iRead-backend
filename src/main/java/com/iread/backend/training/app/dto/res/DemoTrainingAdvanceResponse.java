package com.iread.backend.training.app.dto.res;

public record DemoTrainingAdvanceResponse(
        String action,
        Long curriculumId,
        String curriculumStatus,
        Long completedTrainingId,
        String completedTrainingStatus,
        Long nextTrainingId,
        String nextTrainingStatus
) {
}
