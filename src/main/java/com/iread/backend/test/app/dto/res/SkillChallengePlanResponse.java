package com.iread.backend.test.app.dto.res;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record SkillChallengePlanResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long testCurriculumId,
        int completedQuestions,
        int totalQuestions,
        boolean completed,
        @JsonSerialize(using = ToStringSerializer.class) Long nextTestId,
        String nextTrackCode,
        List<Track> tracks
) {
    public record Track(
            String trackCode,
            String title,
            String status,
            int completedQuestions,
            int totalQuestions,
            @JsonSerialize(using = ToStringSerializer.class) Long nextTestId
    ) {
    }
}
