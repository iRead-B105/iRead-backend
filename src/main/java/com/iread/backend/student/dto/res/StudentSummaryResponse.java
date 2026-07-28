package com.iread.backend.student.dto.res;

public record StudentSummaryResponse(
        long totalStudents,
        long scheduledTodayCount
) {
}
