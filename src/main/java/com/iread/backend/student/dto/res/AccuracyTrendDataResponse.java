package com.iread.backend.student.dto.res;

import java.util.List;

public record AccuracyTrendDataResponse(
        String unit,
        String calculationVersion,
        List<AccuracyTrendResponse> dailyAccuracy
) {
}
