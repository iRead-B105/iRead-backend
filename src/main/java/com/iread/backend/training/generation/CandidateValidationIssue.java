package com.iread.backend.training.generation;

public record CandidateValidationIssue(
        int dataIndex,
        String path,
        String type,
        String message
) {
}
