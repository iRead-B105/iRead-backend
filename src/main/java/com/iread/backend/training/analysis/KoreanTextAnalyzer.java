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
    private static final Pattern WORD_PATTERN = Pattern.compile("[가-힣]+");
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
        List<MorphemeAnalysis> morphemes = morphAnalyzer.analyze(text);
        List<AnalyzedWord> words = analyzeWords(text, g2p);
        return new KoreanTextAnalysis(
                text,
                g2p.pronunciation(),
                List.of("SENTENCE.SIMPLE"),
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
            for (int index = 0; index < surface.length(); index++) {
                addSyllableFeatures(HangulSyllable.decompose(surface.charAt(index)), featureCodes);
            }
            featureCodes.add("WORD.SYLLABLE_COUNT." + Math.min(surface.length(), 5));
            if (!surface.equals(pronunciation)) {
                featureCodes.add("WORD.PHONOLOGICALLY_CHANGED");
            }

            int wordSyllableStart = globalSyllableStart;
            int wordSyllableEnd = wordSyllableStart + surface.length() - 1;
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
            globalSyllableStart += surface.length();
        }
        return words;
    }

    private void addSyllableFeatures(HangulSyllable syllable, Set<String> features) {
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
}
