package com.iread.backend.training.analysis;

import java.util.List;

public record AnalyzedWord(
        int wordIndex,
        String surface,
        List<String> featureCodes,
        List<FeatureOccurrence> featureOccurrences
) {
    public AnalyzedWord {
        featureCodes = List.copyOf(featureCodes);
        featureOccurrences = List.copyOf(featureOccurrences);
    }
}
