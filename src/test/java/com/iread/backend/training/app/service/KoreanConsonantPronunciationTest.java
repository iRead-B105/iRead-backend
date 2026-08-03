package com.iread.backend.training.app.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanConsonantPronunciationTest {
    @ParameterizedTest
    @CsvSource({
            "ㄱ, 그", "ㄲ, 끄", "ㄴ, 느", "ㄷ, 드", "ㄸ, 뜨",
            "ㄹ, 르", "ㅁ, 므", "ㅂ, 브", "ㅃ, 쁘", "ㅅ, 스",
            "ㅆ, 쓰", "ㅇ, 으", "ㅈ, 즈", "ㅉ, 쯔", "ㅊ, 츠",
            "ㅋ, 크", "ㅌ, 트", "ㅍ, 프", "ㅎ, 흐"
    })
    void addsEuToEveryModernInitialConsonant(String consonant, String expected) {
        assertThat(KoreanConsonantPronunciation.withEu(consonant)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"ㅏ", "가", "사과"})
    void leavesNonConsonantTextUnchanged(String text) {
        assertThat(KoreanConsonantPronunciation.withEu(text)).isEqualTo(text);
    }
}
