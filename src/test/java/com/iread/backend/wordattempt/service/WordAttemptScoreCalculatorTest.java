package com.iread.backend.wordattempt.service;

import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordAttemptScoreCalculatorTest {

    private final WordAttemptScoreCalculator calculator = new WordAttemptScoreCalculator(
            new WordAttemptScoreProperties(100, 70, 200, 600, 100, 50, 30, 20)
    );

    @Test
    void 음성만_쓰는_문항은_발음과_수행_가중치를_100퍼센트로_재분배한다() {
        Integer score = calculator.calculate(
                950,
                true,
                true,
                false,
                false,
                false,
                null,
                null,
                0,
                true
        );

        assertThat(score).isEqualTo(964);
    }

    @Test
    void 음성과_시선을_모두_쓰면_설정_가중치를_적용한다() {
        Integer score = calculator.calculate(
                800,
                true,
                true,
                false,
                true,
                true,
                false,
                2,
                1,
                false
        );

        assertThat(score).isEqualTo(780);
    }

    @Test
    void 필요한_시선_입력이_아직_없으면_점수를_확정하지_않는다() {
        Integer score = calculator.calculate(
                900,
                true,
                true,
                false,
                true,
                false,
                null,
                null,
                0,
                true
        );

        assertThat(score).isNull();
    }

    @Test
    void 발음이_없는_문항은_시선과_수행_가중치를_재분배한다() {
        Integer score = calculator.calculate(
                null,
                false,
                false,
                false,
                true,
                true,
                false,
                1,
                0,
                true
        );

        assertThat(score).isEqualTo(940);
    }

    @Test
    void 음성과_시선이_없는_문항은_수행_점수만_사용한다() {
        Integer score = calculator.calculate(
                null,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                0,
                true
        );

        assertThat(score).isEqualTo(1000);
    }
}
