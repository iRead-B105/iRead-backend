package com.iread.backend.ai.dto.req;

import java.util.List;

public record StoryBranchInputReviewRequest(
        String requestId,
        String question,
        List<String> options,
        String transcript
) {
}
