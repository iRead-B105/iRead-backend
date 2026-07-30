package com.iread.backend.training.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KoreanG2pEngine {

    public static final String G2P_VERSION = "G2P_V1";
    public static final String RULE_ENGINE_VERSION = "READING_RULE_V1";

    private static final Map<String, String> NEUTRAL_CODA = neutralCodas();
    private static final Map<String, String> TENSE_ONSET = Map.of(
            "ㄱ", "ㄲ", "ㄷ", "ㄸ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅈ", "ㅉ"
    );
    private static final Map<String, String> ASPIRATED_ONSET = Map.of(
            "ㄱ", "ㅋ", "ㄷ", "ㅌ", "ㅂ", "ㅍ", "ㅈ", "ㅊ"
    );

    public G2pResult convert(String text) {
        List<MutableSyllable> syllables = new ArrayList<>();
        Map<Integer, Integer> charToSyllable = new HashMap<>();
        for (int charIndex = 0; charIndex < text.length(); charIndex++) {
            char value = text.charAt(charIndex);
            if (HangulSyllable.isHangulSyllable(value)) {
                charToSyllable.put(charIndex, syllables.size());
                syllables.add(new MutableSyllable(charIndex, HangulSyllable.decompose(value)));
            }
        }

        List<RuleOccurrence> occurrences = new ArrayList<>();
        for (int index = 0; index < syllables.size() - 1; index++) {
            MutableSyllable current = syllables.get(index);
            MutableSyllable next = syllables.get(index + 1);
            if (!onlyKoreanBoundary(text, current.charIndex, next.charIndex)) {
                continue;
            }
            applyPairRules(current, next, index, occurrences);
        }
        applyNeutralization(syllables, occurrences);

        char[] pronunciation = text.toCharArray();
        for (MutableSyllable syllable : syllables) {
            pronunciation[syllable.charIndex] = syllable.value.compose();
        }
        String result = new String(pronunciation);
        return new G2pResult(result, finalizeOccurrences(text, result, syllables, occurrences));
    }

    private void applyPairRules(MutableSyllable current, MutableSyllable next, int index,
                                List<RuleOccurrence> occurrences) {
        String coda = current.value.coda();
        if (coda == null) {
            return;
        }

        if (("ㄷ".equals(coda) || "ㅌ".equals(coda))
                && "ㅇ".equals(next.value.onset()) && "ㅣ".equals(next.value.vowel())) {
            next.value = next.value.withOnset("ㄷ".equals(coda) ? "ㅈ" : "ㅊ");
            current.value = current.value.withCoda(null);
            String detail = "ㄷ".equals(coda) ? "ㄷ_BEFORE_이" : "ㅌ_BEFORE_이";
            occurrences.add(new RuleOccurrence("PHONOLOGY.PALATALIZATION", index, index + 1));
            occurrences.add(new RuleOccurrence("PHONOLOGY.PALATALIZATION." + detail, index, index + 1));
            return;
        }

        if ("ㅎ".equals(next.value.onset()) && ASPIRATED_ONSET.containsKey(neutral(coda))) {
            next.value = next.value.withOnset(ASPIRATED_ONSET.get(neutral(coda)));
            current.value = current.value.withCoda(null);
            occurrences.add(new RuleOccurrence("PHONOLOGY.ASPIRATION", index, index + 1));
            occurrences.add(new RuleOccurrence("PHONOLOGY.ASPIRATION.WITH_ㅎ", index, index + 1));
            return;
        }

        if ("ㅇ".equals(next.value.onset())) {
            LiaisonParts liaison = liaison(coda);
            if (liaison != null) {
                current.value = current.value.withCoda(liaison.remainingCoda());
                next.value = next.value.withOnset(liaison.movedOnset());
                occurrences.add(new RuleOccurrence("PHONOLOGY.LIAISON", index, index + 1));
                occurrences.add(new RuleOccurrence(
                        "PHONOLOGY.LIAISON.CODA_TO_SILENT_ONSET",
                        index,
                        index + 1
                ));
                return;
            }
        }

        String neutral = neutral(coda);
        if (Set.of("ㄴ", "ㅁ").contains(next.value.onset())
                && Set.of("ㄱ", "ㄷ", "ㅂ").contains(neutral)) {
            String changed = switch (neutral) {
                case "ㄱ" -> "ㅇ";
                case "ㄷ" -> "ㄴ";
                default -> "ㅁ";
            };
            current.value = current.value.withCoda(changed);
            String detail = neutral + "_BEFORE_" + next.value.onset();
            occurrences.add(new RuleOccurrence("PHONOLOGY.NASALIZATION", index, index + 1));
            occurrences.add(new RuleOccurrence("PHONOLOGY.NASALIZATION." + detail, index, index + 1));
            return;
        }

        if (("ㄴ".equals(neutral) && "ㄹ".equals(next.value.onset()))
                || ("ㄹ".equals(neutral) && "ㄴ".equals(next.value.onset()))) {
            String originalNextOnset = next.value.onset();
            current.value = current.value.withCoda("ㄹ");
            next.value = next.value.withOnset("ㄹ");
            String detail = neutral + "_BEFORE_" + originalNextOnset;
            occurrences.add(new RuleOccurrence("PHONOLOGY.LIQUIDIZATION", index, index + 1));
            occurrences.add(new RuleOccurrence("PHONOLOGY.LIQUIDIZATION." + detail, index, index + 1));
            return;
        }

        if (Set.of("ㄱ", "ㄷ", "ㅂ").contains(neutral)
                && TENSE_ONSET.containsKey(next.value.onset())) {
            next.value = next.value.withOnset(TENSE_ONSET.get(next.value.onset()));
            occurrences.add(new RuleOccurrence("PHONOLOGY.TENSIFICATION", index, index + 1));
            occurrences.add(new RuleOccurrence(
                    "PHONOLOGY.TENSIFICATION.AFTER_OBSTRUENT_CODA",
                    index,
                    index + 1
            ));
        }
    }

    private void applyNeutralization(List<MutableSyllable> syllables,
                                     List<RuleOccurrence> occurrences) {
        for (int index = 0; index < syllables.size(); index++) {
            MutableSyllable syllable = syllables.get(index);
            String coda = syllable.value.coda();
            if (coda == null) {
                continue;
            }
            String neutral = neutral(coda);
            if (!coda.equals(neutral)) {
                syllable.value = syllable.value.withCoda(neutral);
                occurrences.add(new RuleOccurrence("PHONOLOGY.FINAL_NEUTRALIZATION", index, index));
                if (Set.of("ㄱ", "ㄷ", "ㅂ").contains(neutral)) {
                    occurrences.add(new RuleOccurrence(
                            "PHONOLOGY.FINAL_NEUTRALIZATION.TO_" + neutral,
                            index,
                            index
                    ));
                }
            }
        }
    }

    private List<FeatureOccurrence> finalizeOccurrences(
            String text,
            String pronunciation,
            List<MutableSyllable> syllables,
            List<RuleOccurrence> raw
    ) {
        List<FeatureOccurrence> result = new ArrayList<>();
        for (RuleOccurrence occurrence : raw) {
            MutableSyllable start = syllables.get(occurrence.start());
            MutableSyllable end = syllables.get(occurrence.end());
            int startChar = start.charIndex;
            int endChar = end.charIndex + 1;
            result.add(new FeatureOccurrence(
                    occurrence.code(),
                    occurrence.start(),
                    occurrence.end(),
                    text.substring(startChar, endChar),
                    pronunciation.substring(startChar, endChar)
            ));
        }
        return result;
    }

    private boolean onlyKoreanBoundary(String text, int currentCharIndex, int nextCharIndex) {
        for (int index = currentCharIndex + 1; index < nextCharIndex; index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private String neutral(String coda) {
        return NEUTRAL_CODA.getOrDefault(coda, coda);
    }

    private LiaisonParts liaison(String coda) {
        return switch (coda) {
            case "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅆ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ" ->
                    new LiaisonParts(null, onsetForLiaison(coda));
            case "ㄳ" -> new LiaisonParts("ㄱ", "ㅅ");
            case "ㄵ" -> new LiaisonParts("ㄴ", "ㅈ");
            case "ㄶ" -> new LiaisonParts("ㄴ", "ㅎ");
            case "ㄺ" -> new LiaisonParts("ㄹ", "ㄱ");
            case "ㄻ" -> new LiaisonParts("ㄹ", "ㅁ");
            case "ㄼ" -> new LiaisonParts("ㄹ", "ㅂ");
            case "ㄽ" -> new LiaisonParts("ㄹ", "ㅅ");
            case "ㄾ" -> new LiaisonParts("ㄹ", "ㅌ");
            case "ㄿ" -> new LiaisonParts("ㄹ", "ㅍ");
            case "ㅀ" -> new LiaisonParts("ㄹ", "ㅎ");
            case "ㅄ" -> new LiaisonParts("ㅂ", "ㅅ");
            default -> null;
        };
    }

    private String onsetForLiaison(String coda) {
        return switch (coda) {
            case "ㄲ" -> "ㄲ";
            case "ㅆ" -> "ㅆ";
            case "ㅋ" -> "ㅋ";
            case "ㅌ" -> "ㅌ";
            case "ㅍ" -> "ㅍ";
            case "ㅎ" -> "ㅎ";
            default -> coda;
        };
    }

    private static Map<String, String> neutralCodas() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String coda : List.of("ㄱ", "ㄲ", "ㄳ", "ㄺ", "ㅋ")) values.put(coda, "ㄱ");
        for (String coda : List.of("ㄴ", "ㄵ", "ㄶ")) values.put(coda, "ㄴ");
        for (String coda : List.of("ㄷ", "ㅅ", "ㅆ", "ㅈ", "ㅊ", "ㅌ", "ㅎ")) values.put(coda, "ㄷ");
        for (String coda : List.of("ㄹ", "ㄼ", "ㄽ", "ㄾ", "ㅀ")) values.put(coda, "ㄹ");
        for (String coda : List.of("ㅁ", "ㄻ")) values.put(coda, "ㅁ");
        for (String coda : List.of("ㅂ", "ㅄ", "ㄿ")) values.put(coda, "ㅂ");
        values.put("ㅇ", "ㅇ");
        return Map.copyOf(values);
    }

    public record G2pResult(String pronunciation, List<FeatureOccurrence> occurrences) {
        public G2pResult {
            occurrences = List.copyOf(occurrences);
        }
    }

    private static final class MutableSyllable {
        private final int charIndex;
        private HangulSyllable value;

        private MutableSyllable(int charIndex, HangulSyllable value) {
            this.charIndex = charIndex;
            this.value = value;
        }
    }

    private record RuleOccurrence(String code, int start, int end) {
    }

    private record LiaisonParts(String remainingCoda, String movedOnset) {
    }
}
