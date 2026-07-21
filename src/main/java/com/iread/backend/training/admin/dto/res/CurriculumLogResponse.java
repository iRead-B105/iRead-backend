package com.iread.backend.training.admin.dto.res;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CurriculumLogResponse(Long curriculumId, LocalDate date, BigDecimal achievement,
                                    List<TrainingItem> trainings) {
    public record TrainingItem(Long trainingId, String unitName, String trainingName) {}
}
