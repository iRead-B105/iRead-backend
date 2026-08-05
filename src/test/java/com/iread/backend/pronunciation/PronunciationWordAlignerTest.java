package com.iread.backend.pronunciation;

import com.iread.backend.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PronunciationWordAlignerTest {

    private final PronunciationWordAligner aligner = new PronunciationWordAligner();

    @Test
    void alignsReferenceWordsAndCountsInsertions() {
        var result = aligner.align(
                List.of(
                        new PronunciationReferenceWord(0, "아기는"),
                        new PronunciationReferenceWord(1, "사과를")
                ),
                List.of(
                        word(0, "아기는", 91.0, "None"),
                        word(1, "정말", 80.0, "Insertion"),
                        word(2, "사과를", null, "Omission")
                )
        );

        assertThat(result.words()).hasSize(2);
        assertThat(result.words().get(1).reference().tokenIndex()).isEqualTo(1);
        assertThat(result.words().get(1).analyzed().scoreOrZero()).isZero();
        assertThat(result.insertionCount()).isEqualTo(1);
    }

    @Test
    void rejectsDifferentWordOrderInsteadOfSavingWrongTokenScore() {
        assertThatThrownBy(() -> aligner.align(
                List.of(
                        new PronunciationReferenceWord(0, "아기는"),
                        new PronunciationReferenceWord(1, "사과를")
                ),
                List.of(
                        word(0, "사과를", 91.0, "None"),
                        word(1, "아기는", 90.0, "None")
                )
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("정렬할 수 없습니다");
    }

    private PronunciationWordResult word(
            int index,
            String value,
            Double score,
            String errorType
    ) {
        return new PronunciationWordResult(index, value, score, errorType, 0, 100);
    }
}
