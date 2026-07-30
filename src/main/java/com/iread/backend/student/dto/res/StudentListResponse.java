package com.iread.backend.student.dto.res;

import java.time.LocalDate;

public record StudentListResponse(
        Long studentId,
        String name,
        String school,
        Integer age,
        String imageUrl,
        String recentTraining,
        LocalDate recentLearningDate,
        long weeklyScheduledCount,
        long weeklyCompletedCount,
        Integer weeklyParticipationRate,
        long totalLearningMinutes
) {
}
