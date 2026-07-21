package com.iread.backend.student.dto.res;

import java.time.LocalDate;

public record StudentListResponse(
        Long id,
        String name,
        Integer age,
        LocalDate recentLearningDate,
        Long totalLearningTime,
        String recentTraining
) {
}
