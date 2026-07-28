package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LearningEventResponse(
        Long eventId,
        String eventType,
        LocalDateTime occurredAt,
        Long sourceId,
        BigDecimal accuracy,
        long retryCount,
        List<String> problemSegments,
        boolean attentionRequired,
        List<String> attentionReasons,
        Long recommendedTrainingTemplateId,
        Long recommendedCurriculumUnitId,
        String recommendedCurriculumUnitName,
        String recommendationReason,
        Integer recommendedMinutes,
        Integer recommendedRepeatCount
) {
}
