package com.iread.backend.training.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class KoreanTextAnalyzer {

    public static final String ANALYZER_VERSION = "KOREAN_ANALYZER_V1";
    private static final Pattern WORD_PATTERN = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣ]+");
    private static final Set<String> ALL_ONSETS = Set.of(
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ",
            "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );
    private static final Set<String> SIMPLE_CODAS = Set.of(
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅆ", "ㅇ", "ㅈ",
            "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );
    private static final Set<String> BASIC_VOWELS = Set.of(
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅛ",
            "ㅜ", "ㅠ", "ㅡ", "ㅣ"
    );
    private static final Set<String> TENSE_ONSETS = Set.of("ㄲ", "ㄸ", "ㅃ", "ㅆ", "ㅉ");
    private static final Set<String> ASPIRATED_ONSETS = Set.of("ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ");
    private static final Set<String> COMPLEX_CODAS = Set.of(
            "ㄳ", "ㄵ", "ㄶ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅄ"
    );

    private final KomoranMorphAnalyzer morphAnalyzer;
    private final KoreanG2pEngine g2pEngine;

    public KoreanTextAnalysis analyze(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("분석할 한글 텍스트는 필수입니다.");
        }

        KoreanG2pEngine.G2pResult g2p = g2pEngine.convert(text);
        List<MorphemeAnalysis> morphemes = text.chars()
                .anyMatch(value -> HangulSyllable.isHangulSyllable((char) value))
                ? morphAnalyzer.analyze(text)
                : List.of();
        List<AnalyzedWord> words = analyzeWords(text, g2p);
        return new KoreanTextAnalysis(
                text,
                g2p.pronunciation(),
                words.stream().anyMatch(word -> word.surface().chars()
                        .anyMatch(value -> HangulSyllable.isHangulSyllable((char) value)))
                        ? List.of("SENTENCE.SIMPLE")
                        : List.of(),
                words,
                morphemes,
                ANALYZER_VERSION,
                KoreanG2pEngine.G2P_VERSION,
                KoreanG2pEngine.RULE_ENGINE_VERSION
        );
    }

    private List<AnalyzedWord> analyzeWords(String text, KoreanG2pEngine.G2pResult g2p) {
        List<AnalyzedWord> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);
        int globalSyllableStart = 0;
        int wordIndex = 0;
        while (matcher.find()) {
            String surface = matcher.group();
            String pronunciation = g2p.pronunciation().substring(matcher.start(), matcher.end());
            LinkedHashSet<String> featureCodes = new LinkedHashSet<>();
            int hangulSyllableCount = 0;
            for (int index = 0; index < surface.length(); index++) {
                char value = surface.charAt(index);
                if (HangulSyllable.isHangulSyllable(value)) {
                    addSyllableFeatures(HangulSyllable.decompose(value), featureCodes);
                    hangulSyllableCount++;
                } else {
                    addStandaloneJamoFeatures(String.valueOf(value), featureCodes);
                }
            }
            if (hangulSyllableCount > 0) {
                featureCodes.add("WORD.SYLLABLE_COUNT." + Math.min(hangulSyllableCount, 5));
            }
            if (!surface.equals(pronunciation)) {
                featureCodes.add("WORD.PHONOLOGICALLY_CHANGED");
            }

            int wordSyllableStart = globalSyllableStart;
            int wordSyllableEnd = wordSyllableStart + hangulSyllableCount - 1;
            List<FeatureOccurrence> occurrences = g2p.occurrences().stream()
                    .filter(value -> value.startSyllableIndex() >= wordSyllableStart
                            && value.endSyllableIndex() <= wordSyllableEnd)
                    .map(value -> new FeatureOccurrence(
                            value.code(),
                            value.startSyllableIndex() - wordSyllableStart,
                            value.endSyllableIndex() - wordSyllableStart,
                            value.orthographicForm(),
                            value.pronunciationForm()
                    ))
                    .toList();
            occurrences.forEach(value -> featureCodes.add(value.code()));
            words.add(new AnalyzedWord(
                    wordIndex++,
                    surface,
                    pronunciation,
                    List.copyOf(featureCodes),
                    occurrences
            ));
            globalSyllableStart += hangulSyllableCount;
        }
        return words;
    }

    private void addSyllableFeatures(HangulSyllable syllable, Set<String> features) {
        features.add("GRAPHEME");
        String onsetKind = TENSE_ONSETS.contains(syllable.onset())
                ? "TENSE"
                : ASPIRATED_ONSETS.contains(syllable.onset()) ? "ASPIRATED" : "BASIC";
        features.add("GRAPHEME.ONSET." + onsetKind + "." + syllable.onset());
        features.add("GRAPHEME.VOWEL."
                + (BASIC_VOWELS.contains(syllable.vowel()) ? "BASIC." : "COMPOUND.")
                + syllable.vowel());
        if (syllable.coda() == null) {
            features.add("SYLLABLE.CV");
        } else {
            features.add("GRAPHEME.CODA."
                    + (COMPLEX_CODAS.contains(syllable.coda()) ? "COMPLEX." : "SIMPLE.")
                    + syllable.coda());
            features.add("SYLLABLE.CVC");
        }
        if (!BASIC_VOWELS.contains(syllable.vowel())) {
            features.add("SYLLABLE.COMPLEX_VOWEL");
        }
        if (TENSE_ONSETS.contains(syllable.onset())) {
            features.add("SYLLABLE.TENSE_ONSET");
        }
        if (syllable.coda() != null && COMPLEX_CODAS.contains(syllable.coda())) {
            features.add("SYLLABLE.COMPLEX_CODA");
        }
    }

    private void addStandaloneJamoFeatures(String jamo, Set<String> features) {
        features.add("GRAPHEME");
        if (BASIC_VOWELS.contains(jamo) || Set.of("ㅘ", "ㅙ", "ㅚ", "ㅝ", "ㅞ", "ㅟ", "ㅢ").contains(jamo)) {
            features.add("GRAPHEME.VOWEL."
                    + (BASIC_VOWELS.contains(jamo) ? "BASIC." : "COMPOUND.")
                    + jamo);
            return;
        }
        if (ALL_ONSETS.contains(jamo)) {
            String onsetKind = TENSE_ONSETS.contains(jamo)
                    ? "TENSE"
                    : ASPIRATED_ONSETS.contains(jamo) ? "ASPIRATED" : "BASIC";
            features.add("GRAPHEME.ONSET." + onsetKind + "." + jamo);
        }
        if (SIMPLE_CODAS.contains(jamo)) {
            features.add("GRAPHEME.CODA.SIMPLE." + jamo);
        } else if (COMPLEX_CODAS.contains(jamo)) {
            features.add("GRAPHEME.CODA.COMPLEX." + jamo);
        }
    }
}
