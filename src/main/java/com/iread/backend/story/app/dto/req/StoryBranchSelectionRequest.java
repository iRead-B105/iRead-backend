package com.iread.backend.story.app.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StoryBranchSelectionRequest(
        @Min(1) @Max(3) Integer optionNo,
        String branchIntent,
        String reviewToken
) {
    public StoryBranchSelectionRequest {
        boolean hasOption = optionNo != null;
        boolean hasIntent = branchIntent != null && !branchIntent.isBlank();
        boolean hasReviewToken = reviewToken != null && !reviewToken.isBlank();
        if (hasOption) {
            if (hasIntent || hasReviewToken) {
                throw new IllegalArgumentException("optionNo는 음성 검토 필드와 함께 사용할 수 없습니다.");
            }
        } else if (!hasIntent || !hasReviewToken) {
            throw new IllegalArgumentException("자유 음성 선택에는 branchIntent와 reviewToken이 필요합니다.");
        }
    }

    public StoryBranchSelectionRequest(Integer optionNo) {
        this(optionNo, null, null);
    }

    public StoryBranchSelectionRequest(String branchIntent, String reviewToken) {
        this(null, branchIntent, reviewToken);
    }
}
