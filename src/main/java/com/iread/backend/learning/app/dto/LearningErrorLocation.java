package com.iread.backend.learning.app.dto;

public record LearningErrorLocation(
        Integer targetIndex,
        Integer tokenIndex,
        String errorCode
) {
}
