package com.iread.backend.student.domain;

import java.util.Locale;

public enum LearningEventType {
    TEST,
    TRAINING,
    STORY,
    GAZE;

    public static LearningEventType fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("eventType은 필수입니다.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "eventType은 test, training, story, gaze 중 하나여야 합니다."
            );
        }
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
