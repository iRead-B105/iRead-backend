package com.iread.backend.training.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanTextAnalyzerTest {

    private final KoreanTextAnalyzer analyzer = new KoreanTextAnalyzer(
            new KomoranMorphAnalyzer(),
            new KoreanG2pEngine()
    );

    @Test
    void createsWordFeaturesOccurrencesAndVersionedAnalysis() {
        KoreanTextAnalysis result = analyzer.analyze("아기는 국물을 먹는다.");

        assertThat(result.words()).hasSize(3);
        assertThat(result.morphemes()).isNotEmpty();
        assertThat(result.analyzerVersion()).isEqualTo("KOREAN_ANALYZER_V1");
        assertThat(result.g2pVersion()).isEqualTo("G2P_V1");
        assertThat(result.ruleEngineVersion()).isEqualTo("READING_RULE_V1");

        AnalyzedWord target = result.words().get(2);
        assertThat(target.wordIndex()).isEqualTo(2);
        assertThat(target.surface()).isEqualTo("먹는다");
        assertThat(target.featureCodes()).contains(
                "GRAPHEME.CODA.SIMPLE.ㄱ",
                "SYLLABLE.CVC",
                "PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ",
                "WORD.PHONOLOGICALLY_CHANGED"
        );
        assertThat(target.featureOccurrences())
                .anySatisfy(value -> {
                    assertThat(value.code()).isEqualTo("PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ");
                    assertThat(value.startSyllableIndex()).isZero();
                    assertThat(value.endSyllableIndex()).isEqualTo(1);
                    assertThat(value.orthographicForm()).isEqualTo("먹는");
                    assertThat(value.pronunciationForm()).isEqualTo("멍는");
                });
    }
}
