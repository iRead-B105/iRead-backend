package com.iread.backend.pronunciation;

import com.iread.backend.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PronunciationWordAligner {

    public Alignment align(
            List<PronunciationReferenceWord> references,
            List<PronunciationWordResult> analyzedWords
    ) {
        List<PronunciationWordResult> insertions = analyzedWords.stream()
                .filter(PronunciationWordResult::isInsertion)
                .toList();
        List<PronunciationWordResult> referenceResults = analyzedWords.stream()
                .filter(word -> !word.isInsertion())
                .toList();
        if (references.size() != referenceResults.size()) {
            throw alignmentFailure();
        }

        List<AlignedWord> aligned = new ArrayList<>();
        for (int index = 0; index < references.size(); index++) {
            PronunciationReferenceWord reference = references.get(index);
            PronunciationWordResult analyzed = referenceResults.get(index);
            if (!normalize(reference.surface()).equals(normalize(analyzed.word()))) {
                throw alignmentFailure();
            }
            aligned.add(new AlignedWord(reference, analyzed));
        }
        return new Alignment(aligned, insertions.size());
    }

    private String normalize(String value) {
        // 자모 기준 단어(ㅏ)는 발화 음절(아)로 평가되므로 비교 전 같은 형태로 맞춘다.
        return Normalizer.normalize(JamoPronunciations.toSpokenText(value), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}ㄱ-ㅎㅏ-ㅣ]", "");
    }

    private ConflictException alignmentFailure() {
        return new ConflictException(
                "발음 분석 결과를 훈련 문항의 단어 순서와 정렬할 수 없습니다."
        );
    }

    public record AlignedWord(
            PronunciationReferenceWord reference,
            PronunciationWordResult analyzed
    ) {
    }

    public record Alignment(
            List<AlignedWord> words,
            int insertionCount
    ) {
        public Alignment {
            words = List.copyOf(words);
            if (insertionCount < 0) {
                throw new IllegalArgumentException("insertionCount는 0 이상이어야 합니다.");
            }
        }
    }
}
