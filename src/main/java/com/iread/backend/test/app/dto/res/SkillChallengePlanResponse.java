package com.iread.backend.test.app.dto.res;

import java.util.List;

public record SkillChallengePlanResponse(
        Long testCurriculumId,
        int completedQuestions,
        int totalQuestions,
        boolean completed,
        Long nextTestId,
        String nextTrackCode,
        List<Track> tracks
) {
    public record Track(
            String trackCode,
            String title,
            String status,
            int completedQuestions,
            int totalQuestions,
            Long nextTestId
    ) {
    }
}
