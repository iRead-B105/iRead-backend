package com.iread.backend.training.analysis;

import java.util.List;

public record HangulSyllable(
        char character,
        String onset,
        String vowel,
        String coda
) {
    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final List<String> ONSETS = List.of(
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ",
            "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );
    private static final List<String> VOWELS = List.of(
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ",
            "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    );
    private static final List<String> CODAS = List.of(
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ",
            "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ",
            "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );

    public HangulSyllable {
        if (!isHangulSyllable(character)) {
            throw new IllegalArgumentException("완성형 한글 음절이 아닙니다: " + character);
        }
        if (!ONSETS.contains(onset) || !VOWELS.contains(vowel)
                || (coda != null && !CODAS.contains(coda))) {
            throw new IllegalArgumentException("유효하지 않은 한글 자모 조합입니다.");
        }
        coda = coda == null || coda.isBlank() ? null : coda;
    }

    public static boolean isHangulSyllable(char value) {
        return value >= HANGUL_BASE && value <= HANGUL_END;
    }

    public static HangulSyllable decompose(char value) {
        if (!isHangulSyllable(value)) {
            throw new IllegalArgumentException("완성형 한글 음절이 아닙니다: " + value);
        }
        int offset = value - HANGUL_BASE;
        String coda = CODAS.get(offset % 28);
        return new HangulSyllable(
                value,
                ONSETS.get(offset / 588),
                VOWELS.get(offset % 588 / 28),
                coda.isEmpty() ? null : coda
        );
    }

    public char compose() {
        int onsetIndex = ONSETS.indexOf(onset);
        int vowelIndex = VOWELS.indexOf(vowel);
        int codaIndex = CODAS.indexOf(coda == null ? "" : coda);
        return (char) (HANGUL_BASE + onsetIndex * 588 + vowelIndex * 28 + codaIndex);
    }

    public HangulSyllable withOnset(String value) {
        return new HangulSyllable(character, value, vowel, coda);
    }

    public HangulSyllable withCoda(String value) {
        return new HangulSyllable(character, onset, vowel, value);
    }
}
