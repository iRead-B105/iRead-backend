package com.iread.backend.student.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LearningEventListItemResponse(
        Long eventId,
        String eventType,
        LocalDateTime occurredAt,
        Long sourceId,
        BigDecimal accuracy,
        boolean attentionRequired,
        List<String> attentionReasons
) {
}
