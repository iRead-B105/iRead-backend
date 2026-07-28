package com.iread.backend.training.generation;

import java.util.List;

public record CandidateValidationResult(
        boolean passed,
        List<CandidateValidationIssue> issues
) {
    public CandidateValidationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        passed = issues.isEmpty();
    }
}
