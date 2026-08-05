package com.iread.backend.reading.analysis;

public record PhonologicalRuleOccurrence(
        PhonologicalRule rule,
        int startIndex,
        int endIndex,
        String context
) {
}
