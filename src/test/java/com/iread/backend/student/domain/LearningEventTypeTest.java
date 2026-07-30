package com.iread.backend.student.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningEventTypeTest {
    @Test
    void parsesOpenApiValuesCaseInsensitively() {
        assertThat(LearningEventType.fromApiValue("test")).isEqualTo(LearningEventType.TEST);
        assertThat(LearningEventType.fromApiValue("TRAINING")).isEqualTo(LearningEventType.TRAINING);
        assertThat(LearningEventType.STORY.apiValue()).isEqualTo("story");
    }

    @Test
    void rejectsMissingOrUnknownValue() {
        assertThatThrownBy(() -> LearningEventType.fromApiValue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType은 필수입니다.");
        assertThatThrownBy(() -> LearningEventType.fromApiValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventType은 test, training, story, gaze 중 하나여야 합니다.");
    }
}
