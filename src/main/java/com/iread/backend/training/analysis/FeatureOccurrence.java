package com.iread.backend.training.analysis;

public record FeatureOccurrence(
        String code,
        int startSyllableIndex,
        int endSyllableIndex,
        String orthographicForm,
        String pronunciationForm
) {
}
