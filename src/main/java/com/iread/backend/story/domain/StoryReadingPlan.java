package com.iread.backend.story.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class StoryReadingPlan {

    public static final int TOTAL_DAYS = 10;
    public static final int PAGES_PER_DAY = 10;
    public static final int TOTAL_PAGES = TOTAL_DAYS * PAGES_PER_DAY;

    private StoryReadingPlan() {
    }

    public static int availableDay(LocalDateTime storyCreatedAt, LocalDate today) {
        if (storyCreatedAt == null) {
            return 1;
        }
        long elapsedDays = Math.max(0, ChronoUnit.DAYS.between(storyCreatedAt.toLocalDate(), today));
        return Math.min(TOTAL_DAYS, Math.toIntExact(elapsedDays) + 1);
    }

    public static int currentDay(int pageCount) {
        if (pageCount <= 0) {
            return 1;
        }
        return Math.min(TOTAL_DAYS, ((pageCount - 1) / PAGES_PER_DAY) + 1);
    }

    public static boolean closesDay(int pageCount) {
        return pageCount > 0 && pageCount % PAGES_PER_DAY == 0;
    }
}
