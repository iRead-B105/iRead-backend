package com.iread.backend.training.analysis;

import java.util.List;

public record KoreanTextAnalysis(
        String text,
        List<String> sentenceFeatureCodes,
        List<AnalyzedWord> words,
        String analyzerVersion,
        String g2pVersion,
        String ruleEngineVersion
) {
    public KoreanTextAnalysis {
        sentenceFeatureCodes = List.copyOf(sentenceFeatureCodes);
        words = List.copyOf(words);
    }
}
