package com.iread.backend.training.app.controller;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.auth.annotation.CurrentStudentId;
import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.security.StudentResourceAccessPolicy;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.repository.TrainingDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * 마이크 없이 발음 평가 경로를 확인하기 위한 시험용 엔드포인트.
 *
 * <p>문항의 기준 문장을 TTS로 읽어 돌려준다. App은 이 음성을 녹음 대신 제출해
 * 브라우저 마이크 문제와 서버 채점 문제를 구분한다. 학습 기록에는 관여하지 않으며
 * {@code iread.training-tts-sample.enabled}가 켜진 환경에서만 등록한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/training/{studentId}/{trainingId}")
@ConditionalOnProperty(
        name = "iread.training-tts-sample.enabled",
        havingValue = "true"
)
public class TrainingTtsSampleController {

    private final TrainingDataRepository trainingDataRepository;
    private final StudentResourceAccessPolicy studentResourceAccessPolicy;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Operation(summary = "[시험용] 문항의 기준 문장을 TTS 음성으로 받는다")
    @PostMapping(value = "/questions/{questionNumber}/tts-sample", produces = "audio/mpeg")
    public ResponseEntity<byte[]> ttsSample(
            @CurrentStudentId Long authenticatedStudentId,
            @PathVariable Long studentId,
            @PathVariable Long trainingId,
            @PathVariable int questionNumber
    ) {
        studentResourceAccessPolicy.requireSameStudent(authenticatedStudentId, studentId);
        String text = expectedText(trainingId, questionNumber);
        SpeechSynthesisResponse speech = aiClient.synthesizeSpeech(
                new SpeechSynthesisRequest(UUID.randomUUID().toString(), text, null)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Audio-Duration-Ms", String.valueOf(speech.durationMs()))
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(speech.audio());
    }

    private String expectedText(Long trainingId, int questionNumber) {
        JsonNode generated = trainingDataRepository.findByTrainingId(trainingId)
                .map(TrainingDataEntity::getGeneratedData)
                .map(objectMapper::readTree)
                .orElseThrow(() -> new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다."));
        JsonNode questions = generated.path("questions");
        if (!questions.isArray() || questionNumber < 1 || questionNumber > questions.size()) {
            throw new ResourceNotFoundException("훈련 문항을 찾을 수 없습니다.");
        }
        JsonNode question = questions.get(questionNumber - 1);
        String text = question.path("answer").path("expectedText").asString();
        if (text == null || text.isBlank()) {
            throw new ResourceNotFoundException("문항에 읽을 문장이 없습니다.");
        }
        return text;
    }
}
