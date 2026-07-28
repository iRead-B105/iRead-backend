package com.iread.backend.reading.analysis;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhonologicalRuleDetectorTest {

    private final PhonologicalRuleDetector detector = new PhonologicalRuleDetector();

    @ParameterizedTest
    @CsvSource({
            "국물, NASALIZATION",
            "옷이, LIAISON",
            "같이, PALATALIZATION",
            "신라, LIQUIDIZATION",
            "국밥, TENSIFICATION",
            "좋다, ASPIRATION",
            "꽃, FINAL_CONSONANT_NEUTRALIZATION"
    })
    void 주요_음운_규칙_후보를_탐지한다(String text, PhonologicalRule expectedRule) {
        assertThat(detector.detect(text))
                .extracting(PhonologicalRuleOccurrence::rule)
                .contains(expectedRule);
    }

    @ParameterizedTest
    @CsvSource({
            "나무",
            "오리",
            "바다"
    })
    void 규칙이_없는_단어는_빈_결과를_반환한다(String text) {
        assertThat(detector.detect(text)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "꽃이",
            "옷이"
    })
    void 모음으로_연음되는_받침은_대표음_규칙으로_분류하지_않는다(String text) {
        assertThat(detector.detect(text))
                .extracting(PhonologicalRuleOccurrence::rule)
                .doesNotContain(PhonologicalRule.FINAL_CONSONANT_NEUTRALIZATION);
    }
}
