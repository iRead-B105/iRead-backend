package com.iread.backend.training.analysis;

public record MorphemeAnalysis(
        String surface,
        String pos,
        int beginIndex,
        int endIndex
) {
}
