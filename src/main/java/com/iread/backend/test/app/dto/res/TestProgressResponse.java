package com.iread.backend.test.app.dto.res;

import java.util.UUID;

public record TestProgressResponse(
        String feedbackType,
        UUID submissionId,
        boolean accepted,
        int questionNumber,
        int completedQuestions,
        int totalQuestions,
        int progressPercent,
        Integer nextQuestionNumber,
        boolean testCompleted
) {
}
