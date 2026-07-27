package com.iread.backend.student.dto.res;

import java.util.List;

public record AccuracyTrendDataResponse(
        List<AccuracyTrendResponse> dailyAccuracy
) {
}
