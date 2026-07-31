package com.iread.backend.story.analysis;

import com.iread.backend.pronunciation.PronunciationReferenceWord;
import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.training.analysis.AnalyzedWord;
import com.iread.backend.training.analysis.FeatureOccurrence;
import com.iread.backend.training.analysis.KoreanTextAnalysis;
import com.iread.backend.training.analysis.KoreanTextAnalyzer;
import com.iread.backend.training.analysis.MorphemeAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 스토리 대사의 {@code content} JSON을 읽고 쓴다.
 *
 * <p>대사는 {@code {"text": "...", "analysis": {...}}} 형태로 저장되며, analysis는 훈련 문항과
 * 동일하게 {@link KoreanTextAnalyzer}가 생성한 형태소·G2P·featureCodes 결과다. JSON 전환 이전에
 * 저장된 평문 대사도 그대로 읽을 수 있고, analysis가 비어 있으면 처음 읽힐 때 채운다.
 */
@Service
@RequiredArgsConstructor
public class StoryLineContentService {

    /** 발음 분석 어댑터의 단어 분리 기준과 같아야 정렬이 맞는다. */
    private static final Pattern REFERENCE_WORD_PATTERN =
            Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+");

    private final KoreanTextAnalyzer analyzer;
    private final ObjectMapper objectMapper;

    /** AI가 생성한 평문 대사를 분석해 저장용 content JSON 문자열로 만든다. */
    public String buildContent(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("스토리 대사 본문은 필수입니다.");
        }
        ObjectNode content = objectMapper.createObjectNode();
        content.put("text", text);
        content.set("analysis", toAnalysisNode(analyzer.analyze(text)));
        return content.toString();
    }

    /** 대사 본문. JSON 전환 이전에 저장된 평문 content도 그대로 읽는다. */
    public String textOf(StoryLineEntity line) {
        String text = readContent(line).path("text").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("스토리 대사 본문이 비어 있습니다.");
        }
        return text;
    }

    /**
     * 대사의 분석 결과. 비어 있으면 지금 분석해 content에 채운 뒤 반환한다.
     * 쓰기 트랜잭션에서 호출해야 보정 결과가 저장된다.
     */
    public JsonNode ensureAnalysis(StoryLineEntity line) {
        JsonNode analysis = readContent(line).path("analysis");
        if (analysis.isObject() && !analysis.isEmpty()) {
            return analysis;
        }
        line.updateContent(buildContent(textOf(line)));
        return readContent(line).path("analysis");
    }

    /** 저장된 분석 결과만 읽는다. 없으면 비어 있는 노드를 돌려주며 보정하지 않는다. */
    public JsonNode analysisOf(StoryLineEntity line) {
        return readContent(line).path("analysis");
    }

    /**
     * 단어 시도 로그 한 건에 해당하는 featureCodes.
     * tokenIndex가 같고 표면형도 같은 항목을 먼저 찾고, 없으면 표면형만 일치하는 항목을 쓴다.
     */
    public List<String> featureCodesOf(JsonNode analysis, Integer tokenIndex, String surfaceText) {
        JsonNode words = analysis == null ? null : analysis.path("words");
        if (words == null || !words.isArray()) {
            return List.of();
        }
        JsonNode fallback = null;
        for (JsonNode word : words) {
            if (!sameSurface(word.path("surface").asText(""), surfaceText)) {
                continue;
            }
            if (tokenIndex != null && word.path("wordIndex").asInt(-1) == tokenIndex) {
                return featureCodes(word);
            }
            if (fallback == null) {
                fallback = word;
            }
        }
        return fallback == null ? List.of() : featureCodes(fallback);
    }

    private List<String> featureCodes(JsonNode word) {
        List<String> codes = new ArrayList<>();
        word.path("featureCodes").forEach(code -> {
            if (code.isTextual() && !code.asText().isBlank()) {
                codes.add(code.asText());
            }
        });
        return List.copyOf(codes);
    }

    /** 발음 분석에 넘길 기준 단어 목록. 어댑터의 단어 분리 기준과 같은 규칙을 쓴다. */
    public List<PronunciationReferenceWord> referenceWords(String text) {
        List<PronunciationReferenceWord> words = new ArrayList<>();
        Matcher matcher = REFERENCE_WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            words.add(new PronunciationReferenceWord(words.size(), matcher.group()));
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("발음 평가할 단어를 찾을 수 없습니다.");
        }
        return List.copyOf(words);
    }

    /**
     * 기준 단어의 tokenIndex별 featureCodes.
     *
     * <p>분석 결과의 단어는 한글만 담고 기준 단어는 숫자·영문도 담으므로, 순서를 유지한 채
     * 표면형이 일치하는 항목끼리 짝지어 준다. 짝이 없는 기준 단어는 featureCodes가 비어 있다.
     */
    public Map<Integer, List<String>> featureCodesByTokenIndex(
            JsonNode analysis,
            List<PronunciationReferenceWord> references
    ) {
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        JsonNode words = analysis == null ? null : analysis.path("words");
        if (words == null || !words.isArray()) {
            return Map.of();
        }
        int cursor = 0;
        for (PronunciationReferenceWord reference : references) {
            while (cursor < words.size()
                    && !sameSurface(words.get(cursor).path("surface").asText(""), reference.surface())) {
                cursor++;
            }
            if (cursor >= words.size()) {
                break;
            }
            List<String> codes = new ArrayList<>();
            words.get(cursor).path("featureCodes").forEach(code -> {
                if (code.isTextual() && !code.asText().isBlank()) {
                    codes.add(code.asText());
                }
            });
            result.put(reference.tokenIndex(), List.copyOf(codes));
            cursor++;
        }
        return Map.copyOf(result);
    }

    /** content를 항상 객체로 읽는다. JSON 전환 이전의 평문 대사는 {@code {"text": ...}}로 감싼다. */
    private ObjectNode readContent(StoryLineEntity line) {
        String content = line.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("스토리 대사 content가 비어 있습니다.");
        }
        String trimmed = content.strip();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.isObject()) {
                    return (ObjectNode) node;
                }
            } catch (Exception exception) {
                // 평문으로 취급한다
            }
        }
        ObjectNode wrapped = objectMapper.createObjectNode();
        wrapped.put("text", content);
        return wrapped;
    }

    private ObjectNode toAnalysisNode(KoreanTextAnalysis analysis) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", analysis.text());
        ArrayNode sentenceFeatureCodes = node.putArray("sentenceFeatureCodes");
        analysis.sentenceFeatureCodes().forEach(sentenceFeatureCodes::add);
        ArrayNode words = node.putArray("words");
        for (AnalyzedWord word : analysis.words()) {
            ObjectNode wordNode = words.addObject();
            wordNode.put("wordIndex", word.wordIndex());
            wordNode.put("surface", word.surface());
            ArrayNode featureCodes = wordNode.putArray("featureCodes");
            word.featureCodes().forEach(featureCodes::add);
            ArrayNode occurrences = wordNode.putArray("featureOccurrences");
            for (FeatureOccurrence occurrence : word.featureOccurrences()) {
                ObjectNode occurrenceNode = occurrences.addObject();
                occurrenceNode.put("code", occurrence.code());
                occurrenceNode.put("startSyllableIndex", occurrence.startSyllableIndex());
                occurrenceNode.put("endSyllableIndex", occurrence.endSyllableIndex());
                occurrenceNode.put("orthographicForm", occurrence.orthographicForm());
                occurrenceNode.put("pronunciationForm", occurrence.pronunciationForm());
            }
        }
        ArrayNode morphemes = node.putArray("morphemes");
        for (MorphemeAnalysis morpheme : analysis.morphemes()) {
            ObjectNode morphemeNode = morphemes.addObject();
            morphemeNode.put("surface", morpheme.surface());
            morphemeNode.put("pos", morpheme.pos());
            morphemeNode.put("beginIndex", morpheme.beginIndex());
            morphemeNode.put("endIndex", morpheme.endIndex());
        }
        node.put("analyzerVersion", analysis.analyzerVersion());
        node.put("g2pVersion", analysis.g2pVersion());
        node.put("ruleEngineVersion", analysis.ruleEngineVersion());
        return node;
    }

    private boolean sameSurface(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
    }
}
