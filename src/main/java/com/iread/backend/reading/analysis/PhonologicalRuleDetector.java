package com.iread.backend.reading.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PhonologicalRuleDetector {

    private static final Set<Character> NASALIZED_FINALS = Set.of('ㄱ', 'ㄲ', 'ㅋ', 'ㄳ', 'ㄺ',
            'ㄷ', 'ㅅ', 'ㅆ', 'ㅈ', 'ㅊ', 'ㅌ', 'ㅎ', 'ㅂ', 'ㅍ', 'ㄼ', 'ㄿ', 'ㅄ');
    private static final Set<Character> NASAL_INITIALS = Set.of('ㄴ', 'ㅁ');
    private static final Set<Character> OBSTRUENT_FINALS = Set.of('ㄱ', 'ㄲ', 'ㅋ', 'ㄳ', 'ㄺ',
            'ㄷ', 'ㅅ', 'ㅆ', 'ㅈ', 'ㅊ', 'ㅌ', 'ㅂ', 'ㅍ', 'ㄼ', 'ㄿ', 'ㅄ');
    private static final Set<Character> TENSIFIABLE_INITIALS = Set.of('ㄱ', 'ㄷ', 'ㅂ', 'ㅅ', 'ㅈ');
    private static final Set<Character> REPRESENTATIVE_FINALS = Set.of('ㄱ', 'ㄴ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅇ');

    private final HangulSyllableAnalyzer syllableAnalyzer;

    public PhonologicalRuleDetector() {
        this(new HangulSyllableAnalyzer());
    }

    PhonologicalRuleDetector(HangulSyllableAnalyzer syllableAnalyzer) {
        this.syllableAnalyzer = syllableAnalyzer;
    }

    public List<PhonologicalRuleOccurrence> detect(String text) {
        List<HangulSyllable> syllables = syllableAnalyzer.decompose(text);
        List<PhonologicalRuleOccurrence> occurrences = new ArrayList<>();

        for (int index = 0; index < syllables.size(); index++) {
            HangulSyllable current = syllables.get(index);
            HangulSyllable next = index + 1 < syllables.size() ? syllables.get(index + 1) : null;
            boolean hasAdjacentNext = next != null && next.textIndex() == current.textIndex() + 1;
            if (requiresFinalConsonantNeutralization(current, hasAdjacentNext ? next : null)) {
                occurrences.add(occurrence(
                        PhonologicalRule.FINAL_CONSONANT_NEUTRALIZATION,
                        current,
                        current,
                        text
                ));
            }
            if (!hasAdjacentNext || !current.hasFinalConsonant()) {
                continue;
            }
            char finalConsonant = current.finalConsonant();

            if (NASALIZED_FINALS.contains(finalConsonant) && NASAL_INITIALS.contains(next.initial())) {
                occurrences.add(occurrence(PhonologicalRule.NASALIZATION, current, next, text));
            }
            if (next.initial() == 'ㅇ') {
                occurrences.add(occurrence(PhonologicalRule.LIAISON, current, next, text));
            }
            if ((finalConsonant == 'ㄷ' || finalConsonant == 'ㅌ')
                    && next.initial() == 'ㅇ' && next.medial() == 'ㅣ') {
                occurrences.add(occurrence(PhonologicalRule.PALATALIZATION, current, next, text));
            }
            if ((finalConsonant == 'ㄴ' && next.initial() == 'ㄹ')
                    || (finalConsonant == 'ㄹ' && next.initial() == 'ㄴ')) {
                occurrences.add(occurrence(PhonologicalRule.LIQUIDIZATION, current, next, text));
            }
            if (OBSTRUENT_FINALS.contains(finalConsonant) && TENSIFIABLE_INITIALS.contains(next.initial())) {
                occurrences.add(occurrence(PhonologicalRule.TENSIFICATION, current, next, text));
            }
            if (isAspiration(finalConsonant, next.initial())) {
                occurrences.add(occurrence(PhonologicalRule.ASPIRATION, current, next, text));
            }
        }

        return List.copyOf(occurrences);
    }

    private boolean requiresFinalConsonantNeutralization(HangulSyllable syllable, HangulSyllable next) {
        return syllable.hasFinalConsonant()
                && !REPRESENTATIVE_FINALS.contains(syllable.finalConsonant())
                && (next == null || next.initial() != 'ㅇ');
    }

    private boolean isAspiration(char finalConsonant, char nextInitial) {
        return (finalConsonant == 'ㅎ' && Set.of('ㄱ', 'ㄷ', 'ㅈ').contains(nextInitial))
                || (nextInitial == 'ㅎ' && Set.of('ㄱ', 'ㄷ', 'ㅂ', 'ㅈ').contains(finalConsonant));
    }

    private PhonologicalRuleOccurrence occurrence(PhonologicalRule rule, HangulSyllable start,
                                                   HangulSyllable end, String text) {
        return new PhonologicalRuleOccurrence(
                rule,
                start.textIndex(),
                end.textIndex(),
                text.substring(start.textIndex(), end.textIndex() + 1)
        );
    }
}
