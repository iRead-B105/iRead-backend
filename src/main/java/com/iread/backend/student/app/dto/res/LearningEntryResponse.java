package com.iread.backend.student.app.dto.res;

public record LearningEntryResponse(
        Long studentId,
        LearningEntryStatus entryStatus,
        Long testCurriculumId,
        int completedQuestions,
        int totalQuestions
) {
}
