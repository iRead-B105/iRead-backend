package com.iread.backend.learning.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.test.repository.TestDataRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.UUID;

/**
 * 그림이 필요한 학습 문항(IMAGE_SENTENCE_MATCH)의 imagePrompt로 삽화를 생성해
 * content.imageUrl에 채운다. 교안·검사 생성 커밋 이후 비동기로 실행되며,
 * 이미지 생성 실패는 학습을 막지 않는다(앱이 imagePrompt 텍스트로 폴백).
 * aiClient.generateImage는 생성 결과를 백엔드 FileStorage에 영속 저장한
 * URL(/uploads/images/*.png)을 돌려주므로 AI 재시작과 무관하게 유지된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningQuestionImagePopulator {

    static final String IMAGE_QUESTION_TYPE = "IMAGE_SENTENCE_MATCH";

    private final TrainingDataRepository trainingDataRepository;
    private final TestDataRepository testDataRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void populateTrainings(List<Long> trainingIds) {
        for (Long trainingId : trainingIds) {
            trainingDataRepository.findByTrainingId(trainingId).ifPresent(data -> {
                String updated = populateQuestions(
                        data.getGeneratedData(),
                        "training-image-" + trainingId
                );
                if (updated != null) {
                    data.updateGeneratedData(updated);
                }
            });
        }
    }

    @Transactional
    public void populateTests(List<Long> testIds) {
        for (Long testId : testIds) {
            testDataRepository.findFirstByTestIdOrderByCreatedAtDescIdDesc(testId).ifPresent(data -> {
                String updated = populateQuestions(
                        data.getGeneratedData(),
                        "test-image-" + testId
                );
                if (updated != null) {
                    data.updateGeneratedData(updated);
                }
            });
        }
    }

    /** 채운 문항이 있으면 갱신된 JSON, 없으면 null. */
    private String populateQuestions(String generatedData, String requestPrefix) {
        ObjectNode root = readObject(generatedData);
        if (root == null) {
            return null;
        }
        boolean changed = false;
        for (JsonNode questionNode : root.path("questions")) {
            if (!(questionNode instanceof ObjectNode question)) {
                continue;
            }
            if (!IMAGE_QUESTION_TYPE.equals(question.path("type").asText())) {
                continue;
            }
            JsonNode contentNode = question.path("content");
            if (!(contentNode instanceof ObjectNode content)) {
                continue;
            }
            String imagePrompt = content.path("imagePrompt").asText("");
            if (imagePrompt.isBlank() || !content.path("imageUrl").asText("").isBlank()) {
                continue;
            }
            try {
                String imageUrl = aiClient.generateImage(new GenerateImageRequest(
                        requestPrefix + "-q" + question.path("questionNo").asInt()
                                + "-" + UUID.randomUUID(),
                        illustrationPrompt(imagePrompt),
                        null
                )).imageUrl();
                if (imageUrl == null || imageUrl.isBlank()) {
                    continue;
                }
                content.put("imageUrl", imageUrl);
                changed = true;
            } catch (Exception exception) {
                // 실패한 문항은 imagePrompt 텍스트 폴백으로 노출된다. 다음 재생성 때 다시 시도.
                log.warn(
                        "학습 문항 삽화 생성 실패 request={} questionNo={}",
                        requestPrefix,
                        question.path("questionNo").asInt(),
                        exception
                );
            }
        }
        return changed ? writeJson(root) : null;
    }

    private String illustrationPrompt(String imagePrompt) {
        return "저학년 아동용 그림책 삽화. 밝고 단순하며 따뜻한 색감으로, "
                + "글자·숫자 없이 다음 장면 하나만 그려줘. 장면: " + imagePrompt.strip();
    }

    private ObjectNode readObject(String value) {
        try {
            var parsed = objectMapper.readTree(value == null || value.isBlank() ? "{}" : value);
            return parsed instanceof ObjectNode object ? object : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("문항 삽화 JSON 저장에 실패했습니다.", exception);
        }
    }
}
