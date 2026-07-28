package com.iread.backend.readingfeature.config;

import com.iread.backend.readingfeature.domain.ReadingFeatureCategory;
import com.iread.backend.readingfeature.domain.ReadingFeatureScope;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iread.reading-feature-seed.enabled", havingValue = "true", matchIfMissing = true)
public class ReadingFeatureDataInitializer implements ApplicationRunner {

    private static final List<String> BASIC_VOWELS = List.of(
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅛ",
            "ㅜ", "ㅠ", "ㅡ", "ㅣ"
    );
    private static final List<String> COMPLEX_VOWELS = List.of(
            "ㅘ", "ㅙ", "ㅚ", "ㅝ", "ㅞ", "ㅟ", "ㅢ"
    );
    private static final List<String> BASIC_ONSETS = List.of(
            "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ"
    );
    private static final List<String> TENSE_ONSETS = List.of("ㄲ", "ㄸ", "ㅃ", "ㅆ", "ㅉ");
    private static final List<String> ASPIRATED_ONSETS = List.of("ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ");
    private static final List<String> SIMPLE_CODAS = List.of(
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );
    private static final List<String> COMPLEX_CODAS = List.of(
            "ㄳ", "ㄵ", "ㄶ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅄ"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        features().forEach(this::insertWhenMissing);
    }

    private void insertWhenMissing(FeatureSeed seed) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reading_features WHERE feature_code = ?",
                Integer.class,
                seed.code()
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO reading_features
                    (id, parent_feature_id, feature_code, feature_name, category, scope, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                seed.id(),
                seed.parentId(),
                seed.code(),
                seed.name(),
                seed.category().name(),
                seed.scope().name()
        );
    }

    private static List<FeatureSeed> features() {
        FeatureSeeds seeds = new FeatureSeeds();

        long grapheme = seeds.add(null, "GRAPHEME", "자모", ReadingFeatureCategory.GRAPHEME,
                ReadingFeatureScope.CHARACTER);
        long onset = seeds.add(grapheme, "GRAPHEME.ONSET", "초성", ReadingFeatureCategory.GRAPHEME,
                ReadingFeatureScope.CHARACTER);
        BASIC_ONSETS.forEach(value -> seeds.add(onset, "GRAPHEME.ONSET.BASIC." + value,
                "기본 초성 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));
        TENSE_ONSETS.forEach(value -> seeds.add(onset, "GRAPHEME.ONSET.TENSE." + value,
                "된소리 초성 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));
        ASPIRATED_ONSETS.forEach(value -> seeds.add(onset, "GRAPHEME.ONSET.ASPIRATED." + value,
                "거센소리 초성 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));

        long vowel = seeds.add(grapheme, "GRAPHEME.VOWEL", "중성", ReadingFeatureCategory.GRAPHEME,
                ReadingFeatureScope.CHARACTER);
        BASIC_VOWELS.forEach(value -> seeds.add(vowel, "GRAPHEME.VOWEL.BASIC." + value,
                "기본 모음 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));
        COMPLEX_VOWELS.forEach(value -> seeds.add(vowel, "GRAPHEME.VOWEL.COMPOUND." + value,
                "복합 모음 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));

        long coda = seeds.add(grapheme, "GRAPHEME.CODA", "종성", ReadingFeatureCategory.GRAPHEME,
                ReadingFeatureScope.CHARACTER);
        SIMPLE_CODAS.forEach(value -> seeds.add(coda, "GRAPHEME.CODA.SIMPLE." + value,
                "홑받침 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));
        COMPLEX_CODAS.forEach(value -> seeds.add(coda, "GRAPHEME.CODA.COMPLEX." + value,
                "겹받침 " + value, ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER));

        long syllable = seeds.add(null, "SYLLABLE", "음절 구조", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);
        seeds.add(syllable, "SYLLABLE.CV", "받침 없는 음절", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);
        seeds.add(syllable, "SYLLABLE.CVC", "받침 있는 음절", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);
        seeds.add(syllable, "SYLLABLE.COMPLEX_VOWEL", "복합 모음 음절", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);
        seeds.add(syllable, "SYLLABLE.TENSE_ONSET", "된소리 초성 음절", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);
        seeds.add(syllable, "SYLLABLE.COMPLEX_CODA", "겹받침 음절", ReadingFeatureCategory.SYLLABLE,
                ReadingFeatureScope.SYLLABLE);

        long phonology = seeds.add(null, "PHONOLOGY", "음운 규칙", ReadingFeatureCategory.PHONOLOGY,
                ReadingFeatureScope.WORD_BOUNDARY);
        addPhonology(seeds, phonology);

        long morph = seeds.add(null, "MORPH", "형태소", ReadingFeatureCategory.MORPH,
                ReadingFeatureScope.WORD);
        seeds.add(morph, "MORPH.NOUN", "명사", ReadingFeatureCategory.MORPH, ReadingFeatureScope.WORD);
        seeds.add(morph, "MORPH.VERB", "동사", ReadingFeatureCategory.MORPH, ReadingFeatureScope.WORD);
        seeds.add(morph, "MORPH.ADJECTIVE", "형용사", ReadingFeatureCategory.MORPH, ReadingFeatureScope.WORD);
        seeds.add(morph, "MORPH.PARTICLE", "조사", ReadingFeatureCategory.MORPH, ReadingFeatureScope.WORD);
        seeds.add(morph, "MORPH.ENDING", "어미", ReadingFeatureCategory.MORPH, ReadingFeatureScope.WORD);

        long word = seeds.add(null, "WORD", "단어", ReadingFeatureCategory.WORD, ReadingFeatureScope.WORD);
        for (int count = 1; count <= 5; count++) {
            seeds.add(word, "WORD.SYLLABLE_COUNT." + count, count + "음절 단어",
                    ReadingFeatureCategory.WORD, ReadingFeatureScope.WORD);
        }
        seeds.add(word, "WORD.PHONOLOGICALLY_CHANGED", "표기와 발음이 다른 단어",
                ReadingFeatureCategory.WORD, ReadingFeatureScope.WORD);
        seeds.add(word, "WORD.AUTOMATICITY", "단어 읽기 자동성",
                ReadingFeatureCategory.WORD, ReadingFeatureScope.WORD);

        long sentence = seeds.add(null, "SENTENCE", "문장", ReadingFeatureCategory.SENTENCE,
                ReadingFeatureScope.SENTENCE);
        seeds.add(sentence, "SENTENCE.SIMPLE", "단순 문장", ReadingFeatureCategory.SENTENCE,
                ReadingFeatureScope.SENTENCE);
        seeds.add(sentence, "SENTENCE.PHRASE_BOUNDARY", "의미 단위 끊어 읽기",
                ReadingFeatureCategory.SENTENCE, ReadingFeatureScope.SENTENCE);
        seeds.add(sentence, "SENTENCE.FLUENCY", "문장 읽기 유창성",
                ReadingFeatureCategory.SENTENCE, ReadingFeatureScope.SENTENCE);

        return seeds.values();
    }

    private static void addPhonology(FeatureSeeds seeds, long root) {
        long nasalization = seeds.add(root, "PHONOLOGY.NASALIZATION", "비음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        for (String code : List.of(
                "ㄱ_BEFORE_ㄴ", "ㄱ_BEFORE_ㅁ", "ㄷ_BEFORE_ㄴ", "ㄷ_BEFORE_ㅁ",
                "ㅂ_BEFORE_ㄴ", "ㅂ_BEFORE_ㅁ"
        )) {
            seeds.add(nasalization, "PHONOLOGY.NASALIZATION." + code, "비음화 " + code,
                    ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        }

        long liaison = seeds.add(root, "PHONOLOGY.LIAISON", "연음",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(liaison, "PHONOLOGY.LIAISON.CODA_TO_SILENT_ONSET", "받침 뒤 모음 연음",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);

        long palatalization = seeds.add(root, "PHONOLOGY.PALATALIZATION", "구개음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(palatalization, "PHONOLOGY.PALATALIZATION.ㄷ_BEFORE_이", "ㄷ 구개음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(palatalization, "PHONOLOGY.PALATALIZATION.ㅌ_BEFORE_이", "ㅌ 구개음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);

        long liquidization = seeds.add(root, "PHONOLOGY.LIQUIDIZATION", "유음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(liquidization, "PHONOLOGY.LIQUIDIZATION.ㄴ_BEFORE_ㄹ", "ㄴ 뒤 ㄹ 유음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(liquidization, "PHONOLOGY.LIQUIDIZATION.ㄹ_BEFORE_ㄴ", "ㄹ 뒤 ㄴ 유음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);

        long tensification = seeds.add(root, "PHONOLOGY.TENSIFICATION", "된소리되기",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(tensification, "PHONOLOGY.TENSIFICATION.AFTER_OBSTRUENT_CODA", "장애음 받침 뒤 된소리",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);

        long aspiration = seeds.add(root, "PHONOLOGY.ASPIRATION", "거센소리되기",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);
        seeds.add(aspiration, "PHONOLOGY.ASPIRATION.WITH_ㅎ", "ㅎ 결합 거센소리",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.WORD_BOUNDARY);

        long neutralization = seeds.add(root, "PHONOLOGY.FINAL_NEUTRALIZATION", "받침 대표음화",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.SYLLABLE);
        seeds.add(neutralization, "PHONOLOGY.FINAL_NEUTRALIZATION.TO_ㄱ", "받침 ㄱ 대표음",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.SYLLABLE);
        seeds.add(neutralization, "PHONOLOGY.FINAL_NEUTRALIZATION.TO_ㄷ", "받침 ㄷ 대표음",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.SYLLABLE);
        seeds.add(neutralization, "PHONOLOGY.FINAL_NEUTRALIZATION.TO_ㅂ", "받침 ㅂ 대표음",
                ReadingFeatureCategory.PHONOLOGY, ReadingFeatureScope.SYLLABLE);
    }

    private record FeatureSeed(
            long id,
            Long parentId,
            String code,
            String name,
            ReadingFeatureCategory category,
            ReadingFeatureScope scope
    ) {
    }

    private static final class FeatureSeeds {
        private final List<FeatureSeed> values = new ArrayList<>();

        long add(Long parentId, String code, String name, ReadingFeatureCategory category,
                 ReadingFeatureScope scope) {
            long id = values.size() + 1L;
            values.add(new FeatureSeed(id, parentId, code, name, category, scope));
            return id;
        }

        List<FeatureSeed> values() {
            return List.copyOf(values);
        }
    }
}
