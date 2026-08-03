package com.iread.backend.story.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StoryReadingPlanTest {

    @Test
    void mapsOneHundredPagesToTenDailyReadingUnits() {
        assertThat(StoryReadingPlan.currentDay(1)).isEqualTo(1);
        assertThat(StoryReadingPlan.currentDay(10)).isEqualTo(1);
        assertThat(StoryReadingPlan.currentDay(11)).isEqualTo(2);
        assertThat(StoryReadingPlan.currentDay(100)).isEqualTo(10);
        assertThat(StoryReadingPlan.closesDay(10)).isTrue();
        assertThat(StoryReadingPlan.closesDay(20)).isTrue();
        assertThat(StoryReadingPlan.closesDay(19)).isFalse();
    }

    @Test
    void opensAtMostOneAdditionalDayPerCalendarDay() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 10, 0);

        assertThat(StoryReadingPlan.availableDay(createdAt, LocalDate.of(2026, 8, 1))).isEqualTo(1);
        assertThat(StoryReadingPlan.availableDay(createdAt, LocalDate.of(2026, 8, 2))).isEqualTo(2);
        assertThat(StoryReadingPlan.availableDay(createdAt, LocalDate.of(2026, 8, 20))).isEqualTo(10);
    }
}
