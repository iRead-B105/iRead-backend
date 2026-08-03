package com.iread.backend.training.app.dto.res;

public record DemoLearningCheatResponse(
        String action,
        Long curriculumId,
        String curriculumStatus,
        int trainingCount
) {
}
