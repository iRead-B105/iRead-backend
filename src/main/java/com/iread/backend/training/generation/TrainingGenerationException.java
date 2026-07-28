package com.iread.backend.training.generation;

import java.util.List;

public class TrainingGenerationException extends RuntimeException {

    private final List<CandidateValidationIssue> issues;

    public TrainingGenerationException(String message, List<CandidateValidationIssue> issues) {
        super(message);
        this.issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public List<CandidateValidationIssue> getIssues() {
        return issues;
    }
}
