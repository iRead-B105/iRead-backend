package com.iread.backend.training.admin.dto.res;

import java.math.BigDecimal;

public record TrainingCatalogResponse(Long trainingId, String category, Integer sequence,
                                      String trainingName, BigDecimal studentAchievementRate) {}
