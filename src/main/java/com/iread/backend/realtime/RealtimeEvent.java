package com.iread.backend.realtime;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public record RealtimeEvent(
        UUID eventId,
        Long studentId,
        RealtimeResource resource,
        Long resourceId,
        String changeType,
        Instant occurredAt,
        long version
) {
    private static final AtomicLong VERSION_SEQUENCE = new AtomicLong();

    public static RealtimeEvent create(
            Long studentId,
            RealtimeResource resource,
            Long resourceId,
            String changeType
    ) {
        Instant occurredAt = Instant.now();
        long version = VERSION_SEQUENCE.updateAndGet(previous ->
                Math.max(occurredAt.toEpochMilli(), previous + 1)
        );
        return new RealtimeEvent(
                UUID.randomUUID(),
                studentId,
                resource,
                resourceId,
                changeType,
                occurredAt,
                version
        );
    }
}
