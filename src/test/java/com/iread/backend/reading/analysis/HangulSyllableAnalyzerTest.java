package com.iread.backend.reading.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HangulSyllableAnalyzerTest {

    private final HangulSyllableAnalyzer analyzer = new HangulSyllableAnalyzer();

    @Test
    void 한글_음절을_초성_중성_종성으로_분해한다() {
        var result = analyzer.decompose("강아지");

        assertThat(result).containsExactly(
                new HangulSyllable(0, '강', 'ㄱ', 'ㅏ', 'ㅇ'),
                new HangulSyllable(1, '아', 'ㅇ', 'ㅏ', null),
                new HangulSyllable(2, '지', 'ㅈ', 'ㅣ', null)
        );
    }

    @Test
    void 비한글은_제외하되_원문_인덱스를_유지한다() {
        var result = analyzer.decompose("A 한글!");

        assertThat(result).extracting(HangulSyllable::textIndex).containsExactly(2, 3);
        assertThat(result).extracting(HangulSyllable::syllable).containsExactly('한', '글');
    }

    @Test
    void null_입력은_거부한다() {
        assertThatThrownBy(() -> analyzer.decompose(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분석할 텍스트가 필요합니다.");
    }
}
