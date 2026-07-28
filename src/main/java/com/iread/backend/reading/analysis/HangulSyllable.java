package com.iread.backend.reading.analysis;

public record HangulSyllable(
        int textIndex,
        char syllable,
        char initial,
        char medial,
        Character finalConsonant
) {
    public boolean hasFinalConsonant() {
        return finalConsonant != null;
    }
}
