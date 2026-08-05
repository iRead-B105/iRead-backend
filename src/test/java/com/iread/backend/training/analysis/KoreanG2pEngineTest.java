package com.iread.backend.training.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanG2pEngineTest {

    private final KoreanG2pEngine engine = new KoreanG2pEngine();

    @Test
    void appliesSevenSupportedPhonologicalRules() {
        assertRule("국물", "궁물", "PHONOLOGY.NASALIZATION");
        assertRule("옷이", "오시", "PHONOLOGY.LIAISON");
        assertRule("같이", "가치", "PHONOLOGY.PALATALIZATION");
        assertRule("신라", "실라", "PHONOLOGY.LIQUIDIZATION");
        assertRule("국밥", "국빱", "PHONOLOGY.TENSIFICATION");
        assertRule("축하", "추카", "PHONOLOGY.ASPIRATION");
        assertRule("옷", "옫", "PHONOLOGY.FINAL_NEUTRALIZATION");
    }

    @Test
    void producesRuleAppliedPronunciationFromProductExample() {
        KoreanG2pEngine.G2pResult result = engine.convert("아기는 국물을 먹는다.");

        assertThat(result.pronunciation()).isEqualTo("아기는 궁무를 멍는다.");
        assertThat(result.occurrences()).extracting(FeatureOccurrence::code)
                .contains(
                        "PHONOLOGY.NASALIZATION",
                        "PHONOLOGY.NASALIZATION.ㄱ_BEFORE_ㄴ",
                        "PHONOLOGY.LIAISON"
                );
    }

    @Test
    void decomposesAndRecomposesEveryModernHangulSyllable() {
        for (char value = 0xAC00; value <= 0xD7A3; value++) {
            assertThat(HangulSyllable.decompose(value).compose()).isEqualTo(value);
        }
        HangulSyllable complex = HangulSyllable.decompose('값');
        assertThat(complex.onset()).isEqualTo("ㄱ");
        assertThat(complex.vowel()).isEqualTo("ㅏ");
        assertThat(complex.coda()).isEqualTo("ㅄ");
    }

    private void assertRule(String source, String expected, String code) {
        KoreanG2pEngine.G2pResult result = engine.convert(source);
        assertThat(result.pronunciation()).isEqualTo(expected);
        assertThat(result.occurrences()).extracting(FeatureOccurrence::code).contains(code);
    }
}
