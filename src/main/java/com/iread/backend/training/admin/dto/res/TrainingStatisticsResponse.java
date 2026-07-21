package com.iread.backend.training.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrainingStatisticsResponse(List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, String trainingName, LocalDate date, BigDecimal accuracy,
                               LocalDate previousTrainingDate, BigDecimal previousAccuracy) {}
}
