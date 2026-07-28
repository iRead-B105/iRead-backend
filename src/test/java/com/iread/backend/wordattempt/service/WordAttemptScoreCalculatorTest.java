package com.iread.backend.wordattempt.service;

import com.iread.backend.wordattempt.config.WordAttemptScoreProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordAttemptScoreCalculatorTest {

    private final WordAttemptScoreCalculator calculator = new WordAttemptScoreCalculator(
            new WordAttemptScoreProperties(100, 300, 700, 200, 600, 250)
    );

    @Test
    void 정확한_첫_응시는_1000점이다() {
        int score = calculator.calculate(950, true, false, 0, true);

        assertThat(score).isEqualTo(1000);
    }

    @Test
    void 재응시_낮은발음점수_오답은_각각_감점한다() {
        int score = calculator.calculate(540, true, false, 2, false);

        assertThat(score).isEqualTo(300);
    }

    @Test
    void 모든_감점을_적용해도_0점_미만으로_내려가지_않는다() {
        int score = calculator.calculate(null, false, true, 10, false);

        assertThat(score).isZero();
    }
}
