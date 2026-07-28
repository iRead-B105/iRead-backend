package com.iread.backend.training.generation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeterministicTrainingCandidateProvider implements TrainingCandidateProvider {

    private static final List<String> WORDS = List.of("사과", "나무", "바다", "토끼", "모자");
    private static final List<String> SENTENCES = List.of(
            "아기는 사과를 먹는다.",
            "누나는 나무를 본다.",
            "토끼가 들판을 달린다.",
            "아이는 모자를 쓴다.",
            "강아지가 문을 닫는다."
    );
    private static final List<String> SYLLABLES = List.of("가", "너", "도", "무", "비");
    private static final List<String> FINALS = List.of("각", "난", "달", "밤", "집");

    private final ObjectMapper objectMapper;

    @Override
    public TrainingCandidateResponse generate(TrainingCandidateRequest request) {
        ArrayNode data = objectMapper.createArrayNode();
        for (int index = 0; index < request.count(); index++) {
            data.add(candidate(request.trainingType(), index));
        }
        return new TrainingCandidateResponse(request.trainingType().name(), data);
    }

    private ObjectNode candidate(TrainingType type, int index) {
        return switch (type) {
            case VOWEL_TRACE -> traceVowel(index);
            case CONSONANT_TRACE -> traceConsonant(index);
            case SYLLABLE_TRACE -> traceSyllable(index);
            case CONSONANT_SOUND_CHOICE -> {
                String target = List.of("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ").get(index);
                yield soundChoice(index, target, List.of("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ"));
            }
            case VOWEL_SOUND_CHOICE -> {
                String target = List.of("ㅏ", "ㅓ", "ㅗ", "ㅜ", "ㅣ").get(index);
                yield soundChoice(index, target, List.of("ㅏ", "ㅓ", "ㅗ", "ㅜ", "ㅣ", "ㅑ", "ㅕ"));
            }
            case CONSONANT_VOWEL_CLASSIFICATION -> classification(index);
            case SYLLABLE_INITIAL_CHOICE -> initialChoice(index, SYLLABLES.get(index));
            case WORD_INITIAL_CHOICE -> initialChoice(index, WORDS.get(index));
            case SAME_INITIAL_WORD_CHOICE -> sameInitialChoice(index);
            case FINAL_CONSONANT_CHOICE -> finalChoice(index);
            case WORD_FINAL_SOUND_CHOICE -> wordFinalChoice(index);
            case FINAL_CONSONANT_COMPARISON -> finalComparison(index);
            case SIMILAR_SOUND_CHOICE -> similarSoundChoice(index);
            case PHONEME_BLEND -> phonemeBlend(index);
            case SYLLABLE_BLEND -> syllableBlend(index);
            case BASIC_SYLLABLE_BUILD -> basicBuild(index);
            case FINAL_SYLLABLE_BUILD -> finalBuild(index, false);
            case DOUBLE_FINAL_BUILD -> finalBuild(index, true);
            case FINAL_CONSONANT_DELETE -> finalDelete(index);
            case SYLLABLE_DELETE -> syllableDelete(index);
            case SYLLABLE_REPLACE -> syllableReplace(index);
            case WORD_READING -> wordReading(index);
            case NONWORD_READING -> nonwordReading(index);
            case DIFFICULT_WORD_PREVIEW -> difficultWordPreview(index);
            case SENTENCE_READING -> sentenceReading(index);
            case SHORT_PASSAGE_READING -> shortPassage(index);
            case SENTENCE_ASSEMBLY -> sentenceAssembly(index);
            case FILL_IN_THE_BLANK -> fillBlank(index);
            case IMAGE_SENTENCE_MATCH -> imageSentenceMatch(index);
            case SENTENCE_REPEAT -> sentenceRepeat(index);
            case WORD_CHAIN_READING -> wordChain(index);
            case PHRASE_READING -> phraseReading(index);
            case REPEATED_SENTENCE_READING -> repeatedSentence(index);
            case SHORT_STORY_READING -> shortStory(index);
        };
    }

    private ObjectNode traceVowel(int index) {
        String target = List.of("ㅏ", "ㅓ", "ㅗ", "ㅜ", "ㅣ").get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("vowelType", "BASIC");
        node.put("target", target);
        node.put("soundText", target);
        node.put("traceAssetKey", "vowel_" + index);
        return node;
    }

    private ObjectNode traceConsonant(int index) {
        String target = List.of("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ").get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("consonantType", "BASIC");
        node.put("target", target);
        node.put("soundText", target);
        node.put("traceAssetKey", "consonant_" + index);
        return node;
    }

    private ObjectNode traceSyllable(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("syllableType", "WITHOUT_FINAL");
        node.put("target", SYLLABLES.get(index));
        node.put("soundText", SYLLABLES.get(index));
        node.put("traceAssetKey", "syllable_" + index);
        return node;
    }

    private ObjectNode soundChoice(int index, String correct, List<String> baseChoices) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", correct);
        List<String> values = choiceValues(correct, baseChoices, index);
        ArrayNode choices = node.putArray("choices");
        values.forEach(choices::add);
        node.put("answerIndex", values.indexOf(correct));
        return node;
    }

    private ObjectNode classification(int index) {
        boolean consonant = index % 2 == 0;
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", consonant ? List.of("ㄱ", "ㄴ", "ㄷ").get(index / 2)
                : List.of("ㅏ", "ㅓ").get(index / 2));
        node.putArray("choices").add("CONSONANT").add("VOWEL");
        node.put("answerIndex", consonant ? 0 : 1);
        return node;
    }

    private ObjectNode initialChoice(int index, String text) {
        String correct = initialOf(text);
        List<String> choices = choiceValues(correct,
                List.of("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅌ"), index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", text);
        choices.forEach(node.putArray("choices")::add);
        node.put("answerIndex", choices.indexOf(correct));
        return node;
    }

    private ObjectNode sameInitialChoice(int index) {
        String target = WORDS.get(index);
        String correct = List.of("수박", "노래", "바구니", "토마토", "무지개").get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("targetType", "WORD");
        node.put("targetAudioText", target);
        node.put("choiceType", "WORD");
        ArrayNode choices = node.putArray("choices");
        List<String> values = rotated(List.of(correct, "기차", "연필", "하늘"), index);
        values.forEach(value -> choices.addObject().put("text", value).put("imagePrompt", ""));
        node.put("answerIndex", values.indexOf(correct));
        return node;
    }

    private ObjectNode finalChoice(int index) {
        String text = FINALS.get(index);
        String correct = finalOf(text);
        List<String> choices = choiceValues(correct, List.of("ㄱ", "ㄴ", "ㄹ", "ㅁ", "ㅂ", "ㅇ"), index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", text);
        choices.forEach(node.putArray("choices")::add);
        node.put("answerIndex", choices.indexOf(correct));
        return node;
    }

    private ObjectNode wordFinalChoice(int index) {
        String word = List.of("산", "달", "밤", "집", "공").get(index);
        String correct = finalOf(word.substring(word.length() - 1));
        List<String> choices = choiceValues(correct, List.of("ㄱ", "ㄴ", "ㄹ", "ㅁ", "ㅂ", "ㅇ"), index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", word);
        choices.forEach(node.putArray("choices")::add);
        node.put("answerIndex", choices.indexOf(correct));
        return node;
    }

    private ObjectNode finalComparison(int index) {
        String audio = FINALS.get(index);
        char syllable = audio.charAt(0);
        int base = syllable - 0xAC00;
        int onsetMedial = base / 28 * 28;
        java.util.LinkedHashSet<String> choiceSet = new java.util.LinkedHashSet<>();
        choiceSet.add(String.valueOf((char) (0xAC00 + onsetMedial + base % 28)));
        for (int codaIndex : List.of(1, 4, 7, 8, 16, 17, 21)) {
            choiceSet.add(String.valueOf((char) (0xAC00 + onsetMedial + codaIndex)));
            if (choiceSet.size() == 4) {
                break;
            }
        }
        List<String> choices = List.copyOf(choiceSet);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("audioText", audio);
        choices.forEach(node.putArray("choices")::add);
        node.put("answerIndex", 0);
        return node;
    }

    private ObjectNode similarSoundChoice(int index) {
        List<String> targets = List.of("가", "다", "바", "자", "사");
        List<String> plains = List.of("ㄱ", "ㄷ", "ㅂ", "ㅈ", "ㅅ");
        String correct = plains.get(index);
        List<String> choices = List.of(correct, aspiratedOf(correct));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("soundGroup", "PLAIN_ASPIRATED");
        node.put("audioText", targets.get(index));
        choices.forEach(node.putArray("choices")::add);
        node.put("answerIndex", 0);
        return node;
    }

    private ObjectNode phonemeBlend(int index) {
        String syllable = SYLLABLES.get(index);
        List<String> parts = decompose(syllable);
        ObjectNode node = objectMapper.createObjectNode();
        parts.forEach(node.putArray("audioParts")::add);
        ArrayNode cards = node.putArray("cards");
        parts.forEach(cards::add);
        cards.add(List.of("ㄹ", "ㅁ", "ㅂ", "ㅅ").stream()
                .filter(value -> !parts.contains(value))
                .findFirst()
                .orElse("ㅎ"));
        ArrayNode order = node.putArray("answerOrder");
        for (int part = 0; part < parts.size(); part++) {
            order.add(part);
        }
        node.put("result", syllable);
        return node;
    }

    private ObjectNode syllableBlend(int index) {
        String word = WORDS.get(index);
        List<String> parts = word.codePoints().mapToObj(Character::toString).toList();
        ObjectNode node = objectMapper.createObjectNode();
        parts.forEach(node.putArray("audioParts")::add);
        ArrayNode cards = node.putArray("cards");
        parts.forEach(cards::add);
        cards.add(List.of("나", "다", "라", "마").stream()
                .filter(value -> !parts.contains(value))
                .findFirst()
                .orElse("하"));
        ArrayNode order = node.putArray("answerOrder");
        for (int part = 0; part < parts.size(); part++) {
            order.add(part);
        }
        node.put("result", word);
        return node;
    }

    private ObjectNode basicBuild(int index) {
        String result = SYLLABLES.get(index);
        List<String> parts = decompose(result);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("targetAudioText", result);
        node.putArray("initialChoices").add(parts.get(0)).add("ㄴ").add("ㅁ");
        node.putArray("medialChoices").add(parts.get(1)).add("ㅏ").add("ㅗ");
        node.put("initialAnswerIndex", 0);
        node.put("medialAnswerIndex", 0);
        node.put("result", result);
        return node;
    }

    private ObjectNode finalBuild(int index, boolean complex) {
        String result = complex
                ? List.of("값", "넋", "닭", "삶", "몫").get(index)
                : FINALS.get(index);
        List<String> parts = decompose(result);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("targetAudioText", result);
        node.putArray("initialChoices").add(parts.get(0)).add("ㄴ").add("ㅁ");
        node.putArray("medialChoices").add(parts.get(1)).add("ㅏ").add("ㅗ");
        node.putArray("finalChoices").add(parts.get(2)).add("ㄴ").add("ㅁ");
        node.put("initialAnswerIndex", 0);
        node.put("medialAnswerIndex", 0);
        node.put("finalAnswerIndex", 0);
        node.put("result", result);
        return node;
    }

    private ObjectNode finalDelete(int index) {
        String source = FINALS.get(index);
        List<String> parts = decompose(source);
        String result = compose(parts.get(0), parts.get(1), null);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("source", source);
        node.put("targetAudioText", result);
        node.putArray("removableUnits").add(parts.get(2)).add("ㄴ").add("ㅁ");
        node.put("answerIndex", 0);
        node.put("result", result);
        return node;
    }

    private ObjectNode syllableDelete(int index) {
        String source = WORDS.get(index);
        List<String> syllables = source.codePoints().mapToObj(Character::toString).toList();
        int deleteIndex = index % syllables.size();
        String result = joinExcept(syllables, deleteIndex);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("source", source);
        node.put("targetAudioText", result);
        syllables.forEach(node.putArray("syllables")::add);
        node.put("deleteIndex", deleteIndex);
        node.put("result", result);
        return node;
    }

    private ObjectNode syllableReplace(int index) {
        String source = List.of("사과", "나비", "바다", "토끼", "모자").get(index);
        String result = List.of("수과", "나무", "바보", "도끼", "과자").get(index);
        int replaceIndex = List.of(0, 1, 1, 0, 0).get(index);
        String replacement = Character.toString(result.charAt(replaceIndex));
        ObjectNode node = objectMapper.createObjectNode();
        node.put("source", source);
        node.put("targetAudioText", result);
        node.put("replaceIndex", replaceIndex);
        node.putArray("choices").add(replacement).add("가").add("나");
        node.put("answerIndex", 0);
        node.put("result", result);
        return node;
    }

    private ObjectNode wordReading(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("readingOrder", index % 2 == 0 ? "SEQUENTIAL" : "FREE");
        rotated(WORDS, index).subList(0, 3).forEach(node.putArray("words")::add);
        return node;
    }

    private ObjectNode nonwordReading(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode words = node.putArray("words");
        words.addObject().put("text", WORDS.get(index)).put("isNonword", false);
        words.addObject().put("text", List.of("나푸", "도미루", "버누", "소마기", "두파").get(index))
                .put("isNonword", true);
        return node;
    }

    private ObjectNode difficultWordPreview(int index) {
        String difficult = List.of("국물", "먹는다", "닫는다", "협력", "꽃잎").get(index);
        String sentence = List.of(
                "아기는 따뜻한 국물을 먹는다.",
                "동생은 아침밥을 먹는다.",
                "아이는 조용히 문을 닫는다.",
                "친구들은 힘을 모아 협력한다.",
                "봄에는 예쁜 꽃잎이 날린다."
        ).get(index);
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode word = node.putArray("difficultWords").addObject();
        word.put("word", difficult);
        difficult.codePoints().mapToObj(Character::toString).forEach(word.putArray("syllables")::add);
        node.put("sentence", sentence);
        return node;
    }

    private ObjectNode sentenceReading(int index) {
        String sentence = SENTENCES.get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sentence", sentence);
        for (String token : sentence.substring(0, sentence.length() - 1).split(" ")) {
            node.putArray("tokens").add(token);
        }
        return node;
    }

    private ObjectNode shortPassage(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode sentences = node.putArray("sentences");
        sentences.add(SENTENCES.get(index));
        sentences.add(List.of(
                "아기는 천천히 꼭꼭 씹었다.",
                "나무 위에서 새가 노래했다.",
                "시원한 파도가 모래를 적셨다.",
                "토끼는 친구와 함께 쉬었다.",
                "모자가 바람에 살짝 흔들렸다."
        ).get(index));
        return node;
    }

    private ObjectNode sentenceAssembly(int index) {
        String sentence = SENTENCES.get(index);
        List<String> ordered = List.of(sentence.split(" "));
        List<String> cards = new java.util.ArrayList<>(ordered);
        java.util.Collections.rotate(cards, 1);
        ObjectNode node = objectMapper.createObjectNode();
        cards.forEach(node.putArray("cards")::add);
        ArrayNode answerOrder = node.putArray("answerOrder");
        ordered.forEach(value -> answerOrder.add(cards.indexOf(value)));
        node.put("completedSentence", sentence);
        return node;
    }

    private ObjectNode fillBlank(int index) {
        String answer = WORDS.get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sentence", "책상 위에 {{blank}} 그림이 있다.");
        node.put("inputType", "CHOICE");
        node.putArray("choices").add(answer).add("기차").add("연필");
        node.put("answerIndex", 0);
        node.putArray("acceptedAnswers").add(answer);
        node.put("completedSentence", "책상 위에 " + answer + " 그림이 있다.");
        return node;
    }

    private ObjectNode imageSentenceMatch(int index) {
        String sentence = SENTENCES.get(index);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("imagePrompt", sentence.substring(0, sentence.length() - 1) + " 장면");
        node.putArray("choices").add(sentence).add("아무도 방에 없다.").add("비가 세차게 내린다.");
        node.put("answerIndex", 0);
        return node;
    }

    private ObjectNode sentenceRepeat(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sentence", SENTENCES.get(index));
        node.put("emotion", index % 2 == 0 ? "HAPPY" : "CALM");
        return node;
    }

    private ObjectNode wordChain(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        rotated(WORDS, index).subList(0, 3).forEach(node.putArray("words")::add);
        node.put("requiredOrder", "SEQUENTIAL");
        return node;
    }

    private ObjectNode phraseReading(int index) {
        String sentence = SENTENCES.get(index);
        String withoutPeriod = sentence.substring(0, sentence.length() - 1);
        String[] tokens = withoutPeriod.split(" ");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sentence", sentence);
        ArrayNode phrases = node.putArray("phrases");
        phrases.add(tokens[0]);
        phrases.add(String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length)) + ".");
        return node;
    }

    private ObjectNode repeatedSentence(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sentence", SENTENCES.get(index));
        node.put("repeatCount", 2 + index % 2);
        return node;
    }

    private ObjectNode shortStory(int index) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("title", WORDS.get(index) + " 이야기");
        ArrayNode sentences = node.putArray("sentences");
        sentences.addObject().put("speaker", "NARRATOR").put("text", SENTENCES.get(index))
                .put("emotion", "CALM");
        sentences.addObject().put("speaker", "CHARACTER").put("text", "오늘은 정말 즐거워!")
                .put("emotion", "HAPPY");
        return node;
    }

    private List<String> rotated(List<String> values, int index) {
        List<String> result = new java.util.ArrayList<>(values);
        java.util.Collections.rotate(result, -index % values.size());
        return result;
    }

    private List<String> choiceValues(String correct, List<String> pool, int index) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        values.add(correct);
        pool.stream().filter(value -> !value.equals(correct)).limit(3).forEach(values::add);
        return rotated(List.copyOf(values), index);
    }

    private String initialOf(String text) {
        List<String> parts = decompose(Character.toString(text.charAt(0)));
        return parts.get(0);
    }

    private String finalOf(String syllable) {
        List<String> parts = decompose(syllable);
        return parts.size() == 3 ? parts.get(2) : "";
    }

    private List<String> decompose(String syllable) {
        char value = syllable.charAt(0);
        int offset = value - 0xAC00;
        if (offset < 0 || offset > 11171) {
            throw new IllegalArgumentException("완성형 한글 음절이 아닙니다: " + syllable);
        }
        String[] onsets = {"ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ",
                "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
        String[] vowels = {"ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ",
                "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"};
        String[] codas = {"", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ",
                "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ",
                "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
        List<String> result = new java.util.ArrayList<>();
        result.add(onsets[offset / 588]);
        result.add(vowels[offset % 588 / 28]);
        if (!codas[offset % 28].isEmpty()) {
            result.add(codas[offset % 28]);
        }
        return result;
    }

    private String compose(String onset, String vowel, String coda) {
        List<String> onsets = List.of("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ",
                "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ");
        List<String> vowels = List.of("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ",
                "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ");
        List<String> codas = List.of("", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ",
                "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ",
                "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ");
        int value = 0xAC00 + onsets.indexOf(onset) * 588 + vowels.indexOf(vowel) * 28
                + codas.indexOf(coda == null ? "" : coda);
        return Character.toString((char) value);
    }

    private String joinExcept(List<String> values, int excluded) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index != excluded) {
                result.append(values.get(index));
            }
        }
        return result.toString();
    }

    private String aspiratedOf(String plain) {
        return switch (plain) {
            case "ㄱ" -> "ㅋ";
            case "ㄷ" -> "ㅌ";
            case "ㅂ" -> "ㅍ";
            case "ㅈ" -> "ㅊ";
            default -> "ㅎ";
        };
    }

    private String tenseOf(String plain) {
        return switch (plain) {
            case "ㄱ" -> "ㄲ";
            case "ㄷ" -> "ㄸ";
            case "ㅂ" -> "ㅃ";
            case "ㅈ" -> "ㅉ";
            default -> "ㅆ";
        };
    }
}
