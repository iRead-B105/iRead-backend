package com.iread.backend.test.app.dto.req;

import com.iread.backend.learning.app.dto.LearningSubmission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TestSubmissionRequest(
        @NotNull Long testId,
        @NotNull @Valid LearningSubmission submission
) {
}
