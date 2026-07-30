package com.iread.backend.reading.analysis;

import java.util.ArrayList;
import java.util.List;

public final class HangulSyllableAnalyzer {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int MEDIAL_COUNT = 21;
    private static final int FINAL_COUNT = 28;

    private static final char[] INITIALS = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char[] MEDIALS = {
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    private static final char[] FINALS = {
            '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public List<HangulSyllable> decompose(String text) {
        if (text == null) {
            throw new IllegalArgumentException("분석할 텍스트가 필요합니다.");
        }

        List<HangulSyllable> syllables = new ArrayList<>();
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (!isHangulSyllable(value)) {
                continue;
            }
            int offset = value - HANGUL_BASE;
            int initialIndex = offset / (MEDIAL_COUNT * FINAL_COUNT);
            int medialIndex = (offset % (MEDIAL_COUNT * FINAL_COUNT)) / FINAL_COUNT;
            int finalIndex = offset % FINAL_COUNT;
            syllables.add(new HangulSyllable(
                    index,
                    value,
                    INITIALS[initialIndex],
                    MEDIALS[medialIndex],
                    finalIndex == 0 ? null : FINALS[finalIndex]
            ));
        }
        return List.copyOf(syllables);
    }

    public boolean isHangulSyllable(char value) {
        return value >= HANGUL_BASE && value <= HANGUL_END;
    }
}
