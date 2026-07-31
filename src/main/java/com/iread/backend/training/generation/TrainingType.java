package com.iread.backend.training.generation;

import java.util.Locale;

public enum TrainingType {
    VOWEL_TRACE,
    CONSONANT_TRACE,
    SYLLABLE_TRACE,
    CONSONANT_SOUND_CHOICE,
    VOWEL_SOUND_CHOICE,
    CONSONANT_VOWEL_CLASSIFICATION,
    SYLLABLE_INITIAL_CHOICE,
    WORD_INITIAL_CHOICE,
    SAME_INITIAL_WORD_CHOICE,
    FINAL_CONSONANT_CHOICE,
    WORD_FINAL_SOUND_CHOICE,
    FINAL_CONSONANT_COMPARISON,
    SIMILAR_SOUND_CHOICE,
    PHONEME_BLEND,
    SYLLABLE_BLEND,
    BASIC_SYLLABLE_BUILD,
    FINAL_SYLLABLE_BUILD,
    DOUBLE_FINAL_BUILD,
    HANGUL_BATTLE_BASIC,
    HANGUL_BATTLE_FINAL,
    HANGUL_BATTLE_DOUBLE_FINAL,
    FINAL_CONSONANT_DELETE,
    SYLLABLE_DELETE,
    SYLLABLE_REPLACE,
    WORD_READING,
    NONWORD_READING,
    DIFFICULT_WORD_PREVIEW,
    SENTENCE_READING,
    SHORT_PASSAGE_READING,
    SENTENCE_ASSEMBLY,
    FILL_IN_THE_BLANK,
    IMAGE_SENTENCE_MATCH,
    SENTENCE_REPEAT,
    WORD_CHAIN_READING,
    PHRASE_READING,
    REPEATED_SENTENCE_READING,
    SHORT_STORY_READING;

    public static TrainingType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("trainingType은 필수입니다.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 trainingType입니다: " + value);
        }
    }
}
