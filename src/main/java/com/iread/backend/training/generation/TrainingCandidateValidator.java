package com.iread.backend.training.generation;

import com.iread.backend.training.analysis.HangulSyllable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TrainingCandidateValidator {

    public CandidateValidationResult validate(
            TrainingCandidateRequest request,
            TrainingCandidateResponse response
    ) {
        List<CandidateValidationIssue> issues = new ArrayList<>();
        if (!request.trainingType().name().equals(response.type())) {
            issues.add(issue(-1, "$.type", "TYPE_MISMATCH",
                    "응답 type이 요청 trainingType과 일치하지 않습니다."));
        }
        if (response.data().size() != request.count()) {
            issues.add(issue(-1, "$.data", "COUNT_MISMATCH",
                    "data 배열은 정확히 " + request.count() + "개여야 합니다."));
        }

        JsonNode itemTemplate = request.outputTemplate().path("data").path(0);
        if (!itemTemplate.isObject()) {
            issues.add(issue(-1, "$.outputTemplate.data[0]", "INVALID_TEMPLATE",
                    "outputTemplate.data[0] 객체가 필요합니다."));
            return new CandidateValidationResult(false, issues);
        }

        Set<String> candidates = new HashSet<>();
        for (int index = 0; index < response.data().size(); index++) {
            JsonNode candidate = response.data().get(index);
            String rootPath = "$.data[" + index + "]";
            validateShape(index, rootPath, itemTemplate, candidate, issues);
            validateIndices(index, rootPath, candidate, issues);
            validateTypeRules(request.trainingType(), index, rootPath, candidate, issues);
            if (!candidates.add(candidate.toString())) {
                issues.add(issue(index, rootPath, "DUPLICATE_QUESTION",
                        "같은 data 배열에서 문항을 중복할 수 없습니다."));
            }
        }
        return new CandidateValidationResult(issues.isEmpty(), issues);
    }

    private void validateShape(int index, String path, JsonNode template, JsonNode value,
                               List<CandidateValidationIssue> issues) {
        if (template.isTextual() && isPlaceholder(template.asText())) {
            validatePlaceholder(index, path, template.asText(), value, issues);
            return;
        }
        if (template.isObject()) {
            if (!value.isObject()) {
                issues.add(issue(index, path, "TYPE_MISMATCH", "JSON 객체여야 합니다."));
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = template.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!value.has(field.getKey())) {
                    issues.add(issue(index, path + "." + field.getKey(), "REQUIRED_FIELD_MISSING",
                            "필수 필드가 없습니다."));
                    continue;
                }
                validateShape(index, path + "." + field.getKey(), field.getValue(),
                        value.get(field.getKey()), issues);
            }
            value.properties().forEach(field -> {
                if (!template.has(field.getKey())) {
                    issues.add(issue(index, path + "." + field.getKey(), "UNEXPECTED_FIELD",
                            "outputTemplate에 없는 필드는 출력할 수 없습니다."));
                }
            });
            return;
        }
        if (template.isArray()) {
            if (!value.isArray()) {
                issues.add(issue(index, path, "TYPE_MISMATCH", "JSON 배열이어야 합니다."));
                return;
            }
            if (!template.isEmpty() && value.isEmpty()) {
                issues.add(issue(index, path, "EMPTY_ARRAY", "배열은 한 개 이상의 항목이 필요합니다."));
            }
            if (!template.isEmpty()) {
                for (int itemIndex = 0; itemIndex < value.size(); itemIndex++) {
                    validateShape(index, path + "[" + itemIndex + "]", template.get(0),
                            value.get(itemIndex), issues);
                }
            }
        }
    }

    private void validatePlaceholder(int index, String path, String placeholder, JsonNode value,
                                     List<CandidateValidationIssue> issues) {
        boolean valid = switch (placeholder) {
            case "<string>" -> value.isTextual();
            case "<integer>" -> value.isIntegralNumber();
            case "<boolean>" -> value.isBoolean();
            case "<number>" -> value.isNumber();
            default -> true;
        };
        if (!valid) {
            issues.add(issue(index, path, "TYPE_MISMATCH",
                    placeholder + " 자료형과 일치하지 않습니다."));
        }
    }

    private void validateIndices(int index, String path, JsonNode candidate,
                                 List<CandidateValidationIssue> issues) {
        validateIndex(index, path, candidate, "answerIndex",
                firstExisting(candidate, "choices", "removableUnits"), issues, true);
        validateIndex(index, path, candidate, "initialAnswerIndex", candidate.path("initialChoices"),
                issues, false);
        validateIndex(index, path, candidate, "medialAnswerIndex", candidate.path("medialChoices"),
                issues, false);
        validateIndex(index, path, candidate, "finalAnswerIndex", candidate.path("finalChoices"),
                issues, false);
        validateIndex(index, path, candidate, "deleteIndex", candidate.path("syllables"),
                issues, false);

        if (candidate.has("answerOrder")) {
            JsonNode cards = candidate.path("cards");
            JsonNode order = candidate.path("answerOrder");
            if (cards.isArray() && order.isArray()) {
                Set<Integer> used = new HashSet<>();
                for (JsonNode item : order) {
                    int value = item.asInt(-1);
                    if (value < 0 || value >= cards.size() || !used.add(value)) {
                        issues.add(issue(index, path + ".answerOrder", "INVALID_INDEX",
                                "answerOrder는 cards의 중복 없는 0 기반 인덱스여야 합니다."));
                        break;
                    }
                }
            }
        }
        validateNoDuplicates(index, path, candidate, "choices", issues);
        validateNoDuplicates(index, path, candidate, "cards", issues);
    }

    private void validateIndex(int index, String path, JsonNode candidate, String field,
                               JsonNode values, List<CandidateValidationIssue> issues,
                               boolean allowDirectInputMinusOne) {
        if (!candidate.has(field)) {
            return;
        }
        int answerIndex = candidate.path(field).asInt(Integer.MIN_VALUE);
        if (allowDirectInputMinusOne && answerIndex == -1 && values.isArray() && values.isEmpty()) {
            return;
        }
        if (!values.isArray() || answerIndex < 0 || answerIndex >= values.size()) {
            issues.add(issue(index, path + "." + field, "INVALID_INDEX",
                    field + "는 연결된 배열의 0 기반 인덱스여야 합니다."));
        }
    }

    private void validateNoDuplicates(int index, String path, JsonNode candidate, String field,
                                      List<CandidateValidationIssue> issues) {
        JsonNode values = candidate.path(field);
        if (!values.isArray()) {
            return;
        }
        Set<String> unique = new HashSet<>();
        for (JsonNode value : values) {
            if (!unique.add(value.toString())) {
                issues.add(issue(index, path + "." + field, "DUPLICATE_OPTION",
                        field + " 안의 항목은 중복할 수 없습니다."));
                return;
            }
        }
    }

    private void validateTypeRules(TrainingType type, int index, String path, JsonNode candidate,
                                   List<CandidateValidationIssue> issues) {
        switch (type) {
            case CONSONANT_VOWEL_CLASSIFICATION -> {
                JsonNode choices = candidate.path("choices");
                Set<String> values = new HashSet<>();
                choices.forEach(value -> values.add(value.asText()));
                if (choices.size() != 2 || !values.equals(Set.of("CONSONANT", "VOWEL"))) {
                    issues.add(issue(index, path + ".choices", "INVALID_CLASSIFICATION_CHOICES",
                            "choices는 CONSONANT과 VOWEL 두 값이어야 합니다."));
                }
            }
            case FINAL_CONSONANT_DELETE ->
                    validateFinalConsonantDelete(index, path, candidate, issues);
            case WORD_CHAIN_READING -> requireText(index, path, candidate, "requiredOrder",
                    "SEQUENTIAL", issues);
            case FILL_IN_THE_BLANK -> {
                String sentence = candidate.path("sentence").asText();
                if (occurrences(sentence, "{{blank}}") != 1) {
                    issues.add(issue(index, path + ".sentence", "INVALID_BLANK",
                            "sentence에는 {{blank}}가 정확히 한 번 있어야 합니다."));
                }
            }
            case REPEATED_SENTENCE_READING -> {
                if (candidate.path("repeatCount").asInt() < 2) {
                    issues.add(issue(index, path + ".repeatCount", "OUT_OF_RANGE",
                            "repeatCount는 2 이상이어야 합니다."));
                }
            }
            default -> {
            }
        }
    }

    private void validateFinalConsonantDelete(
            int index,
            String path,
            JsonNode candidate,
            List<CandidateValidationIssue> issues
    ) {
        String source = candidate.path("source").asText();
        if (source.length() != 1 || !HangulSyllable.isHangulSyllable(source.charAt(0))) {
            issues.add(issue(index, path + ".source", "INVALID_FINAL_DELETE_SOURCE",
                    "source는 받침이 있는 한글 한 음절이어야 합니다."));
            return;
        }
        HangulSyllable syllable = HangulSyllable.decompose(source.charAt(0));
        if (syllable.coda() == null) {
            issues.add(issue(index, path + ".source", "INVALID_FINAL_DELETE_SOURCE",
                    "source는 받침이 있는 한글 한 음절이어야 합니다."));
            return;
        }
        List<String> expectedUnits = List.of(
                syllable.onset(),
                syllable.vowel(),
                syllable.coda()
        );
        JsonNode units = candidate.path("removableUnits");
        boolean unitsMatch = units.isArray() && units.size() == expectedUnits.size();
        for (int unitIndex = 0; unitsMatch && unitIndex < expectedUnits.size(); unitIndex++) {
            unitsMatch = expectedUnits.get(unitIndex).equals(units.get(unitIndex).asText());
        }
        if (!unitsMatch || candidate.path("answerIndex").asInt(-1) != 2) {
            issues.add(issue(index, path + ".removableUnits", "INVALID_FINAL_DELETE_UNITS",
                    "removableUnits는 source의 초성, 중성, 종성 순서이고 정답은 종성이어야 합니다."));
        }
        String expectedResult = Character.toString(
                new HangulSyllable(
                        syllable.character(),
                        syllable.onset(),
                        syllable.vowel(),
                        null
                ).compose()
        );
        if (!expectedResult.equals(candidate.path("result").asText())
                || !expectedResult.equals(candidate.path("targetAudioText").asText())) {
            issues.add(issue(index, path + ".result", "INVALID_FINAL_DELETE_RESULT",
                    "result와 targetAudioText는 source에서 종성을 제거한 음절이어야 합니다."));
        }
    }

    private void requireText(int index, String path, JsonNode candidate, String field,
                             String expected, List<CandidateValidationIssue> issues) {
        if (!expected.equals(candidate.path(field).asText())) {
            issues.add(issue(index, path + "." + field, "INVALID_ENUM",
                    field + "는 " + expected + "여야 합니다."));
        }
    }

    private JsonNode firstExisting(JsonNode candidate, String... fields) {
        for (String field : fields) {
            if (candidate.has(field)) {
                return candidate.path(field);
            }
        }
        return candidate.path("__missing__");
    }

    private boolean isPlaceholder(String value) {
        return value.startsWith("<") && value.endsWith(">");
    }

    private int occurrences(String source, String target) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(target, from)) >= 0) {
            count++;
            from += target.length();
        }
        return count;
    }

    private CandidateValidationIssue issue(int index, String path, String type, String message) {
        return new CandidateValidationIssue(index, path, type, message);
    }
}
