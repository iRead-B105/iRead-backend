package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.dto.res.GenerateTrainingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MockTrainingGenerator {

    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final List<String> DEFAULT_WORDS = List.of("사과", "나무", "하늘", "바다", "토끼");

    private final ObjectMapper objectMapper;

    public GenerateTrainingResponse generate(GenerateTrainingRequest request) {
        JsonNode generationSpec = request.inputData().path("generationSpec");
        int questionCount = generationSpec.path("questionCount").asInt(DEFAULT_QUESTION_COUNT);
        if (questionCount < 1) {
            questionCount = DEFAULT_QUESTION_COUNT;
        }

        ObjectNode generatedData = objectMapper.createObjectNode();
        generatedData.put("version", request.schemaVersion());
        generatedData.put(
                "questionType",
                generationSpec.path("questionType").asText("MOCK_READING")
        );
        ArrayNode questions = generatedData.putArray("questions");

        for (int index = 0; index < questionCount; index++) {
            String word = wordAt(request.inputData().path("expectedWords"), index);
            ObjectNode question = questions.addObject();
            question.put("questionId", "q-%03d".formatted(index + 1));
            question.put("sequence", index + 1);

            ObjectNode problem = question.putObject("problem");
            problem.put("instruction", word + "를 소리 내어 읽어 보세요.");
            problem.put("targetText", word);

            ObjectNode answer = question.putObject("answer");
            answer.put("canonicalText", word);
            answer.put("correctText", word);
        }

        return new GenerateTrainingResponse(
                request.requestId(),
                request.schemaVersion(),
                generatedData
        );
    }

    private String wordAt(JsonNode expectedWords, int index) {
        if (expectedWords.isArray() && !expectedWords.isEmpty()) {
            JsonNode word = expectedWords.get(index % expectedWords.size());
            String value = word.isTextual()
                    ? word.asText()
                    : word.path("wordName").asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        return DEFAULT_WORDS.get(index % DEFAULT_WORDS.size());
    }
}
