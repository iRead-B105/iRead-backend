package com.iread.backend.training.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/** 프론트와 AI 서버가 공통으로 사용하는 고정 템플릿과 AI 생성 명세를 준비한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iread.training-template-seed.enabled", havingValue = "true", matchIfMissing = true)
public class TrainingTemplateDataInitializer implements ApplicationRunner {

    private static final long PHONOLOGY_UNIT_ID = 1L;
    private static final long FLUENCY_UNIT_ID = 2L;

    private static final List<TemplateSeed> TEMPLATES = List.of(
            seed(1, PHONOLOGY_UNIT_ID, "글자 따라 보기", 1, "LETTER_GAZE_TRACE",
                    "글자의 획을 순서대로 바라본 뒤 실제 소릿값을 발음한다.",
                    List.of("모음·자음·음절을 고르게 출제한다.", "자음 이름이 아닌 실제 소릿값을 정답으로 사용한다.", "획 정보는 glyphAssetId로 참조한다."),
                    List.of("targetText", "targetKind", "glyphAssetId", "audioScript"),
                    List.of("requiredStrokeOrder", "acceptedPronunciations", "pronunciationThreshold")),
            seed(2, PHONOLOGY_UNIT_ID, "같은 초성 카드 선택", 2, "SAME_INITIAL_CARD_SELECTION",
                    "들은 음절 또는 단어와 같은 초성으로 시작하는 단어를 선택한다.",
                    List.of("정답은 정확히 하나만 만든다.", "오답은 목표와 다른 초성을 사용한다.", "선택지는 중복되지 않아야 한다."),
                    List.of("targetAudioScript", "choices"), List.of("correctChoiceId", "targetInitial", "correctText")),
            seed(3, PHONOLOGY_UNIT_ID, "소리 듣고 글자·단어 선택", 3, "SOUND_MATCH_CARD_SELECTION",
                    "들은 소리에 해당하는 글자·음절·단어 카드를 선택한다.",
                    List.of("정답은 정확히 하나만 만든다.", "목표 글자는 문제 영역에 노출하지 않는다.", "오답은 발음 또는 형태가 유사한 항목으로 만든다."),
                    List.of("targetAudioScript", "targetKind", "choices"), List.of("correctChoiceId", "canonicalText")),
            seed(4, PHONOLOGY_UNIT_ID, "소리 듣고 쓰기", 4, "LISTEN_HANDWRITING",
                    "들은 글자·음절·단어를 직접 쓰게 한다.",
                    List.of("목표 글자는 문제에 노출하지 않는다.", "한 문제에는 하나의 명확한 표기 정답만 둔다."),
                    List.of("targetAudioScript", "targetKind", "canvasSyllableCount"),
                    List.of("canonicalText", "acceptedTexts", "minimumRecognitionScore")),
            seed(5, PHONOLOGY_UNIT_ID, "소리 듣고 말하기", 5, "LISTEN_SPEAKING",
                    "들은 음절 또는 단어를 따라 말하게 한다.",
                    List.of("연령에 적합하고 발음이 명확한 항목을 사용한다.", "허용 발음과 대표 표기를 함께 제공한다."),
                    List.of("targetAudioScript", "targetKind"),
                    List.of("canonicalText", "acceptedPronunciations", "minimumPronunciationScore")),
            seed(6, PHONOLOGY_UNIT_ID, "자모 조합", 6, "JAMO_COMBINATION",
                    "전체 소리를 듣고 자모 카드를 초성·중성·종성 위치에 배치한다.",
                    List.of("받침 없는 음절부터 복합 모음·된소리 음절까지 출제한다.", "정답에 쓰이지 않는 자모 카드를 포함한다.", "각 슬롯에 정답 카드가 하나만 있어야 한다."),
                    List.of("targetAudioScript", "slots", "cards"), List.of("placements", "canonicalText")),
            seed(7, PHONOLOGY_UNIT_ID, "음절 조합", 7, "SYLLABLE_COMBINATION",
                    "전체 소리를 듣고 음절 카드를 순서대로 배치해 단어 또는 비단어를 만든다.",
                    List.of("실제 단어와 발음 가능한 비단어를 사용할 수 있다.", "방해 음절 카드를 포함한다.", "정답 카드 순서는 하나로 결정되어야 한다."),
                    List.of("targetAudioScript", "targetKind", "slotCount", "cards"),
                    List.of("orderedCardIds", "canonicalText")),
            seed(8, PHONOLOGY_UNIT_ID, "음절 생략", 8, "SYLLABLE_DELETION",
                    "단어에서 음절 하나를 제거해 들은 목표 소리를 만든다.",
                    List.of("2~3음절 원본을 사용한다.", "첫·가운데·마지막 위치를 고르게 출제한다.", "제거할 음절은 정확히 하나다."),
                    List.of("sourceText", "sourceAudioScript", "targetAudioScript", "segments"),
                    List.of("removeSegmentId", "canonicalText")),
            seed(9, PHONOLOGY_UNIT_ID, "초성 생략", 9, "INITIAL_DELETION",
                    "음절의 초성을 제거하고 빈 초성에 ㅇ을 넣어 목표 음절을 만든다.",
                    List.of("중성은 제거 대상으로 만들지 않는다.", "초성 제거 후 ㅇ 삽입 결과를 정답으로 제공한다."),
                    List.of("sourceText", "sourceAudioScript", "targetAudioScript", "segments"),
                    List.of("removeSegmentId", "insertedInitial", "canonicalText")),
            seed(10, PHONOLOGY_UNIT_ID, "종성 생략", 10, "FINAL_DELETION",
                    "받침이 있는 음절에서 종성을 제거해 목표 음절을 만든다.",
                    List.of("종성이 있는 원본만 사용한다.", "중성은 제거 대상으로 만들지 않는다."),
                    List.of("sourceText", "sourceAudioScript", "targetAudioScript", "segments"),
                    List.of("removeSegmentId", "canonicalText")),
            substitution(11, "초성 대치", "INITIAL_SUBSTITUTION", "초성", 11),
            substitution(12, "중성 대치", "MEDIAL_SUBSTITUTION", "중성", 12),
            substitution(13, "종성 대치", "FINAL_SUBSTITUTION", "종성", 13),
            seed(14, PHONOLOGY_UNIT_ID, "음절 대치", 14, "SYLLABLE_SUBSTITUTION",
                    "단어 또는 비단어에서 음절 하나를 바꾸어 목표 소리를 만든다.",
                    List.of("한 문제에서 음절 하나만 바꾼다.", "첫·가운데·마지막 위치를 고르게 출제한다.", "정답 음절 카드와 방해 카드를 함께 제공한다."),
                    List.of("sourceText", "targetAudioScript", "segments", "cards"),
                    List.of("targetSegmentId", "replacementCardId", "canonicalText")),
            seed(15, FLUENCY_UNIT_ID, "단어·비단어 읽기", 1, "WORD_GRID_READING",
                    "2×2로 제시된 단어·비단어 네 개를 자유로운 순서로 읽는다.",
                    List.of("항상 네 항목을 생성한다.", "실제 단어·비단어·혼합 구성을 지원한다.", "비단어는 한국어로 발음 가능해야 한다."),
                    List.of("readingType", "items"), List.of("requiredItemIds", "pronunciationTargets", "orderRequired")),
            seed(16, FLUENCY_UNIT_ID, "문장 읽기", 2, "SENTENCE_READING",
                    "한 문장을 첫 단어부터 순서대로 읽는다.",
                    List.of("연령과 난이도에 맞는 자연스러운 한 문장을 만든다.", "문장을 단어 토큰으로 분리한다."),
                    List.of("readingType", "tokens"), List.of("orderedTokenIds", "canonicalText", "pronunciationTargets")),
            seed(17, FLUENCY_UNIT_ID, "긴 글 읽기", 3, "PASSAGE_READING",
                    "2~3문장으로 구성된 짧은 글을 순서대로 읽는다.",
                    List.of("서로 이어지는 2~3문장을 만든다.", "문장과 단어 토큰의 순서를 명확히 제공한다."),
                    List.of("readingType", "sentences"), List.of("orderedTokenIds", "canonicalText", "pronunciationTargets")),
            cloze(18, "빈칸 카드 선택", "CLOZE_CHOICE", "CARD_SELECTION", 4,
                    List.of("sentence", "choices"), List.of("correctChoiceId", "correctText", "completedSentence")),
            cloze(19, "빈칸 직접 쓰기", "CLOZE_HANDWRITING", "HANDWRITING", 5,
                    List.of("sentence", "blankSyllableCount"), List.of("canonicalText", "acceptedTexts", "completedSentence")),
            cloze(20, "빈칸 말하기", "CLOZE_SPEAKING", "SPEAKING", 6,
                    List.of("sentence"), List.of("canonicalText", "acceptedPronunciations", "completedSentence")),
            seed(21, FLUENCY_UNIT_ID, "문장 순서 조립", 7, "SENTENCE_ORDERING",
                    "섞인 단어 카드를 배열해 자연스러운 문장을 완성한다.",
                    List.of("정답 문장 순서는 하나로 명확해야 한다.", "카드의 문장 내 단어를 빠뜨리거나 중복하지 않는다."),
                    List.of("cards"), List.of("orderedCardIds", "canonicalText")),
            seed(22, FLUENCY_UNIT_ID, "그림과 문장 연결", 8, "PICTURE_SENTENCE_MATCH",
                    "그림을 가장 정확히 설명하는 문장을 선택한다.",
                    List.of("그림 프롬프트는 인물·행동·장소·상황을 명확히 묘사한다.", "정답 문장은 하나만 존재해야 한다.", "오답은 그림과 명확히 불일치해야 한다."),
                    List.of("imagePrompt", "choices"), List.of("correctChoiceId", "correctText"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureFormJsonColumn();
        upsertCurriculumUnit(PHONOLOGY_UNIT_ID, "음운 인식 및 파닉스", 1);
        upsertCurriculumUnit(FLUENCY_UNIT_ID, "짧은 글 및 유창성", 2);
        TEMPLATES.forEach(this::upsertTemplate);
    }

    private void ensureFormJsonColumn() {
        String dataType = jdbcTemplate.queryForObject("""
                SELECT DATA_TYPE
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'training_templates'
                  AND column_name = 'form'
                """, String.class);
        if ("json".equalsIgnoreCase(dataType)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE training_templates MODIFY COLUMN form LONGTEXT NOT NULL");
        jdbcTemplate.execute("""
                UPDATE training_templates
                SET form = JSON_OBJECT('questionType', form)
                WHERE JSON_VALID(form) = 0
                """);
        jdbcTemplate.execute("ALTER TABLE training_templates MODIFY COLUMN form JSON NOT NULL");
    }

    private void upsertCurriculumUnit(long id, String name, int sequenceNo) {
        jdbcTemplate.update("""
                INSERT INTO curriculum_units (id, unit_name, sequence_no) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE unit_name = VALUES(unit_name), sequence_no = VALUES(sequence_no)
                """, id, name, sequenceNo);
    }

    private void upsertTemplate(TemplateSeed seed) {
        jdbcTemplate.update("""
                INSERT INTO training_templates (id, curriculum_unit_id, name, form, sequence_no)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE curriculum_unit_id = VALUES(curriculum_unit_id), name = VALUES(name),
                    form = VALUES(form), sequence_no = VALUES(sequence_no)
                """, seed.id(), seed.curriculumUnitId(), seed.name(), toJson(seed), seed.sequenceNo());
    }

    private String toJson(TemplateSeed seed) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("questionType", seed.questionType());
        root.put("questionCount", 5);
        root.put("objective", seed.objective());
        ArrayNode rules = root.putArray("rules");
        commonRules().forEach(rules::add);
        seed.rules().forEach(rules::add);
        typeRules(seed.questionType()).forEach(rules::add);
        ObjectNode output = root.putObject("outputFormat");
        output.put("questionId", "string");
        output.put("sequence", "integer");
        ObjectNode problem = output.putObject("problem");
        seed.problemFields().forEach(field -> problem.put(field, fieldType(field)));
        ObjectNode answer = output.putObject("answer");
        seed.answerFields().forEach(field -> answer.put(field, fieldType(field)));
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 템플릿 form JSON 생성에 실패했습니다.", exception);
        }
    }

    private List<String> commonRules() {
        return List.of(
                "questions 배열에 정확히 questionCount개의 문제를 생성한다.",
                "questionId는 q-001 형식의 문자열로 만들고 모든 문제에서 중복되지 않게 한다.",
                "sequence는 1부터 시작하는 연속된 정수이며 questions 배열 순서와 일치시킨다.",
                "각 문제는 outputFormat에 명시된 problem과 answer 필드를 빠짐없이 포함한다.",
                "problem에는 학습자에게 제시할 데이터만, answer에는 서버 채점에 필요한 정답만 넣는다.",
                "answer의 ID와 값은 problem에 실제로 존재하는 항목을 참조하며 서로 모순되지 않아야 한다.",
                "같은 세트 안에서 목표 글자·단어·문장과 선택지 구성을 중복 생성하지 않는다.",
                "한국어 학습 아동에게 적합한 일상 어휘와 자연스러운 문장을 사용하고 비속어·폭력적·선정적 내용을 제외한다.",
                "audioScript에는 TTS가 그대로 읽을 한글 텍스트만 넣고 설명, 따옴표, 효과음 표기를 넣지 않는다.",
                "expectedWords가 제공되면 학습 목표와 규칙에 맞는 범위에서 우선 활용하되 규칙을 위반하면서 사용하지 않는다.",
                "JSON 이외의 설명문이나 마크다운을 출력하지 않는다."
        );
    }

    private List<String> typeRules(String type) {
        return switch (type) {
            case "LETTER_GAZE_TRACE" -> List.of(
                    "targetKind는 BASIC_VOWEL, COMPOUND_VOWEL, BASIC_CONSONANT, TENSE_CONSONANT, ASPIRATED_CONSONANT, SYLLABLE 중 하나다.",
                    "glyphAssetId는 targetText와 동일한 글자의 사전 등록 획 자산 ID를 사용하고 AI가 획 좌표를 임의 생성하지 않는다.",
                    "requiredStrokeOrder에는 자산의 모든 strokeId를 실제 획 순서대로 한 번씩 넣는다.",
                    "자음의 acceptedPronunciations에는 기역·니은 같은 자음 이름을 넣지 않고 실제 소릿값만 넣는다.",
                    "audioScript와 acceptedPronunciations는 targetText의 실제 발음과 일치해야 한다.",
                    "pronunciationThreshold는 0 이상 1 이하의 숫자로 생성한다."
            );
            case "SAME_INITIAL_CARD_SELECTION" -> List.of(
                    "choices는 정확히 4개이며 choiceId는 c1부터 c4까지 중복 없이 부여한다.",
                    "정답 단어 하나만 목표 음절 또는 단어와 같은 실제 초성 소리로 시작해야 한다.",
                    "나머지 세 오답은 목표와 서로 다른 초성으로 시작하고 정답으로 해석될 가능성이 없어야 한다.",
                    "correctChoiceId는 choices의 정답 choiceId를 참조하고 correctText는 해당 choice의 text와 완전히 같아야 한다.",
                    "targetInitial은 targetAudioScript와 정답 단어를 한글 분해했을 때의 초성과 모두 일치해야 한다.",
                    "targetAudioScript에는 목표 글자를 보여 주는 안내문 없이 목표 음절 또는 단어 자체만 넣는다."
            );
            case "SOUND_MATCH_CARD_SELECTION" -> List.of(
                    "choices는 정확히 4개이며 정답은 targetAudioScript와 표기 및 발음이 일치하는 하나만 둔다.",
                    "correctChoiceId는 choices에 존재해야 하고 canonicalText는 해당 choice의 text와 완전히 같아야 한다.",
                    "오답 세 개는 목표와 중복되지 않으며 유사한 난이도의 실제 글자·음절·단어로 구성한다.",
                    "targetKind와 모든 choice의 단위는 LETTER, SYLLABLE, WORD 중 하나의 동일한 단위로 통일한다.",
                    "problem에는 정답을 직접 나타내는 targetText 또는 correctText 필드를 넣지 않는다."
            );
            case "LISTEN_HANDWRITING" -> List.of(
                    "targetKind는 LETTER, SYLLABLE, WORD 중 하나이며 audioScript와 canonicalText는 같은 대상을 나타낸다.",
                    "acceptedTexts에는 canonicalText를 반드시 포함하고 실제로 허용할 표기만 중복 없이 넣는다.",
                    "canvasSyllableCount는 canonicalText의 한글 음절 수와 같아야 한다.",
                    "minimumRecognitionScore는 0 이상 1 이하의 숫자로 생성한다.",
                    "problem에는 canonicalText나 acceptedTexts를 노출하지 않는다."
            );
            case "LISTEN_SPEAKING" -> List.of(
                    "targetKind는 SYLLABLE 또는 WORD이며 targetAudioScript와 canonicalText는 동일한 발화 대상이어야 한다.",
                    "acceptedPronunciations에는 canonicalText의 표준 발음을 반드시 포함한다.",
                    "서로 다른 발음이 모두 정답이 되는 동음이의·발음 모호 항목은 피한다.",
                    "minimumPronunciationScore는 0 이상 1 이하의 숫자로 생성한다."
            );
            case "JAMO_COMBINATION" -> List.of(
                    "targetAudioScript는 완성 음절 전체만 포함하고 자모를 나누어 읽지 않는다.",
                    "slots는 INITIAL, MEDIAL을 반드시 포함하고 받침이 있는 문제만 FINAL을 포함한다.",
                    "cards에는 각 정답 자모 카드와 최소 2개의 방해 카드를 포함하며 cardId와 text가 중복되지 않아야 한다.",
                    "placements의 각 키는 slots의 role이고 각 값은 cards에 존재하는 cardId여야 한다.",
                    "placements대로 자모를 조합한 한글 음절은 canonicalText 및 targetAudioScript와 정확히 일치해야 한다.",
                    "복합 모음과 된소리는 하나의 카드 text로 표현한다."
            );
            case "SYLLABLE_COMBINATION" -> List.of(
                    "targetKind는 WORD 또는 PSEUDOWORD 중 하나다.",
                    "slotCount는 canonicalText의 음절 수와 같고 2 이상 4 이하로 생성한다.",
                    "cards에는 정답 음절을 모두 포함하고 최소 2개의 방해 음절을 추가한다.",
                    "orderedCardIds의 길이는 slotCount와 같고 모든 값은 cards의 cardId를 참조한다.",
                    "orderedCardIds 순서로 card text를 결합한 결과가 canonicalText와 정확히 일치해야 한다.",
                    "PSEUDOWORD는 실제 사전에 있는 단어가 아니면서 한국어 음절 규칙에 따라 발음 가능해야 한다."
            );
            case "SYLLABLE_DELETION" -> List.of(
                    "segments는 sourceText를 음절 단위로 왼쪽부터 분리한 배열이며 결합 결과가 sourceText와 같아야 한다.",
                    "각 segment는 고유 segmentId, text, order를 포함한다.",
                    "removeSegmentId는 segments 중 정확히 하나를 참조한다.",
                    "해당 음절을 제거하고 나머지를 순서대로 결합한 결과가 canonicalText와 같아야 한다.",
                    "targetAudioScript는 canonicalText와 같고 problem에는 canonicalText를 직접 노출하지 않는다."
            );
            case "INITIAL_DELETION" -> List.of(
                    "sourceText는 한 글자 한글 음절이고 segments는 INITIAL, MEDIAL과 선택적인 FINAL 조각을 포함한다.",
                    "removeSegmentId는 role이 INITIAL인 segment를 참조해야 한다.",
                    "insertedInitial은 반드시 ㅇ이며 중성과 종성은 원본과 동일하게 유지한다.",
                    "ㅇ과 남은 중성·종성을 조합한 결과가 canonicalText 및 targetAudioScript와 같아야 한다.",
                    "MEDIAL segment는 제거 가능한 정답으로 만들지 않는다."
            );
            case "FINAL_DELETION" -> List.of(
                    "sourceText는 종성이 있는 한 글자 한글 음절이어야 한다.",
                    "segments는 INITIAL, MEDIAL, FINAL을 각각 하나씩 포함하고 removeSegmentId는 FINAL을 참조한다.",
                    "초성과 중성을 그대로 조합한 결과가 canonicalText 및 targetAudioScript와 같아야 한다.",
                    "INITIAL과 MEDIAL segment는 제거 가능한 정답으로 만들지 않는다."
            );
            case "INITIAL_SUBSTITUTION" -> substitutionRules("INITIAL", "초성", "중성과 종성");
            case "MEDIAL_SUBSTITUTION" -> substitutionRules("MEDIAL", "중성", "초성과 종성");
            case "FINAL_SUBSTITUTION" -> substitutionRules("FINAL", "종성", "초성과 중성");
            case "SYLLABLE_SUBSTITUTION" -> List.of(
                    "sourceText는 2~4음절의 단어 또는 발음 가능한 비단어다.",
                    "segments는 sourceText를 음절 단위로 분리한 배열이고 각 조각에 고유 segmentId와 order를 부여한다.",
                    "targetSegmentId가 가리키는 음절 하나만 replacementCardId의 음절로 바꾼다.",
                    "cards에는 정답 음절 카드 하나와 최소 2개의 중복 없는 방해 카드를 포함한다.",
                    "대치하지 않은 모든 음절은 sourceText와 동일해야 한다.",
                    "대치 결과가 canonicalText 및 targetAudioScript와 정확히 일치해야 한다."
            );
            case "WORD_GRID_READING" -> List.of(
                    "items는 정확히 4개이며 각 항목에 고유 itemId, text, itemType을 포함한다.",
                    "itemType은 WORD 또는 PSEUDOWORD이고 요청된 구성에 맞게 실제 단어와 비단어를 배분한다.",
                    "requiredItemIds는 네 itemId를 빠짐없이 한 번씩 포함하고 orderRequired는 false다.",
                    "pronunciationTargets는 itemId를 키로 하고 해당 text의 허용 발음 배열을 값으로 갖는 객체다.",
                    "비단어는 실제 단어와 중복되지 않고 한글로 자연스럽게 발음 가능해야 한다."
            );
            case "SENTENCE_READING" -> List.of(
                    "tokens는 문장을 띄어쓰기 단위로 왼쪽부터 분리하고 각 token에 고유 tokenId, text, order를 포함한다.",
                    "orderedTokenIds는 tokens의 순서를 그대로 참조하며 누락과 중복이 없어야 한다.",
                    "token text를 띄어쓰기로 결합한 결과는 canonicalText와 문장부호를 제외하고 일치해야 한다.",
                    "pronunciationTargets는 각 tokenId별 허용 발음 배열을 포함한다.",
                    "문장은 하나이며 아동이 한 화면에서 읽을 수 있는 길이로 만든다."
            );
            case "PASSAGE_READING" -> List.of(
                    "sentences는 서로 의미가 이어지는 2개 또는 3개의 문장으로 구성한다.",
                    "각 문장은 sentenceId와 순서가 있는 token 배열을 포함한다.",
                    "모든 tokenId는 글 전체에서 고유하며 orderedTokenIds는 문장 순서대로 모든 token을 참조한다.",
                    "canonicalText는 sentences를 순서대로 결합한 전체 글과 일치해야 한다.",
                    "pronunciationTargets는 모든 tokenId에 대한 허용 발음 배열을 포함한다."
            );
            case "CLOZE_CHOICE" -> List.of(
                    "sentence에는 빈칸 표시 ______를 정확히 한 번 포함한다.",
                    "choices는 정확히 4개이며 고유 choiceId와 중복되지 않는 text를 포함한다.",
                    "correctChoiceId의 text만 문맥과 문법에 모두 맞는 유일한 정답이어야 한다.",
                    "correctText는 정답 choice text와 같고 completedSentence는 빈칸을 correctText로 치환한 문장과 같아야 한다."
            );
            case "CLOZE_HANDWRITING" -> List.of(
                    "sentence에는 빈칸 표시 ______를 정확히 한 번 포함하고 정답은 한 단어로 제한한다.",
                    "acceptedTexts에는 canonicalText를 반드시 포함하고 실제 허용 가능한 표기만 넣는다.",
                    "blankSyllableCount는 canonicalText의 한글 음절 수와 같아야 한다.",
                    "completedSentence는 빈칸을 canonicalText로 치환한 결과와 정확히 같아야 한다."
            );
            case "CLOZE_SPEAKING" -> List.of(
                    "sentence에는 빈칸 표시 ______를 정확히 한 번 포함하고 정답은 한 단어로 제한한다.",
                    "acceptedPronunciations에는 canonicalText의 표준 발음을 반드시 포함한다.",
                    "다른 단어도 자연스럽게 들어갈 수 있는 모호한 문장을 만들지 않는다.",
                    "completedSentence는 빈칸을 canonicalText로 치환한 결과와 정확히 같아야 한다."
            );
            case "SENTENCE_ORDERING" -> List.of(
                    "cards는 자연스러운 한 문장을 구성하는 모든 어절을 각각 하나의 카드로 포함한다.",
                    "각 cardId와 text는 중복되지 않고 불필요한 방해 카드는 추가하지 않는다.",
                    "orderedCardIds는 모든 cardId를 정확히 한 번씩 포함한다.",
                    "orderedCardIds 순서로 text를 결합한 결과가 canonicalText와 문장부호를 제외하고 일치해야 한다.",
                    "두 가지 이상의 자연스러운 배열이 가능한 문장을 만들지 않는다."
            );
            case "PICTURE_SENTENCE_MATCH" -> List.of(
                    "imagePrompt는 한 장면만 묘사하고 인물·핵심 행동·장소·중요 사물을 구체적으로 포함한다.",
                    "choices는 정확히 4개의 문장이며 choiceId와 text가 중복되지 않아야 한다.",
                    "정답 문장 하나만 imagePrompt의 모든 핵심 요소와 일치해야 한다.",
                    "오답 세 문장은 인물, 행동, 장소, 사물 중 적어도 하나가 그림과 명확히 달라야 한다.",
                    "correctChoiceId는 정답 choice를 참조하고 correctText는 해당 text와 완전히 같아야 한다."
            );
            default -> throw new IllegalArgumentException("지원하지 않는 훈련 questionType입니다: " + type);
        };
    }

    private List<String> substitutionRules(String role, String koreanRole, String preservedRoles) {
        return List.of(
                "sourceText는 대치 대상 " + koreanRole + "을 포함하는 한 글자 한글 음절이다.",
                "segments는 INITIAL, MEDIAL과 필요한 경우 FINAL을 포함하고 targetSegmentId는 role이 " + role + "인 조각을 참조한다.",
                "cards에는 원본과 다른 정답 " + koreanRole + " 카드 하나와 최소 2개의 중복 없는 방해 카드를 포함한다.",
                "replacementCardId는 cards에 존재하며 해당 카드의 자모 역할은 " + role + "이어야 한다.",
                preservedRoles + "은 sourceText와 동일하게 유지하고 " + koreanRole + " 하나만 변경한다.",
                "교체 결과가 canonicalText 및 targetAudioScript와 정확히 일치해야 한다."
        );
    }

    private String fieldType(String field) {
        if (field.equals("choices") || field.equals("segments") || field.equals("cards")
                || field.equals("slots") || field.equals("items") || field.equals("tokens")
                || field.equals("sentences") || field.equals("requiredStrokeOrder")
                || field.equals("acceptedTexts") || field.equals("acceptedPronunciations")
                || field.equals("orderedCardIds")
                || field.equals("orderedTokenIds") || field.equals("requiredItemIds")) {
            return "array";
        }
        if (field.equals("placements") || field.equals("pronunciationTargets")) {
            return "object";
        }
        if (field.endsWith("Threshold") || field.startsWith("minimum")) {
            return "number";
        }
        if (field.equals("slotCount") || field.equals("canvasSyllableCount")
                || field.equals("blankSyllableCount")) {
            return "integer";
        }
        if (field.equals("orderRequired")) {
            return "boolean";
        }
        return "string";
    }

    private static TemplateSeed substitution(long id, String name, String type, String unit, int sequence) {
        return seed(id, PHONOLOGY_UNIT_ID, name, sequence, type,
                "음절 또는 단어의 " + unit + " 한 조각을 바꾸어 목표 소리를 만든다.",
                List.of("한 문제에서 한 조각만 바꾼다.", "바꾸지 않는 자모는 원본과 같아야 한다.", "정답 카드와 방해 카드를 함께 제공한다."),
                List.of("sourceText", "targetAudioScript", "segments", "cards"),
                List.of("targetSegmentId", "replacementCardId", "canonicalText"));
    }

    private static TemplateSeed cloze(long id, String name, String type, String mode, int sequence,
                                      List<String> problemFields, List<String> answerFields) {
        return seed(id, FLUENCY_UNIT_ID, name, sequence, type,
                "문맥에 맞는 한 단어 또는 짧은 구를 " + mode + " 방식으로 채운 뒤 완성 문장을 읽는다.",
                List.of("빈칸은 하나만 만든다.", "문맥상 정답이 하나로 명확해야 한다.", "완성된 문장은 자연스럽고 연령에 적합해야 한다."),
                problemFields, answerFields);
    }

    private static TemplateSeed seed(long id, long unitId, String name, int sequence, String questionType,
                                     String objective, List<String> rules,
                                     List<String> problemFields, List<String> answerFields) {
        return new TemplateSeed(id, unitId, name, sequence, questionType, objective, rules, problemFields, answerFields);
    }

    private record TemplateSeed(long id, long curriculumUnitId, String name, int sequenceNo,
                                String questionType, String objective, List<String> rules,
                                List<String> problemFields, List<String> answerFields) {
    }
}
