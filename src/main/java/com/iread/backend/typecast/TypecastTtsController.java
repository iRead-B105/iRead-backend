package com.iread.backend.typecast;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.auth.annotation.CurrentStudentId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "학습 앱 TTS", description = "Typecast 음성 합성 프록시 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/tts")
public class TypecastTtsController {
    private final TypecastTtsClient typecastTtsClient;
    private final AiClient aiClient;
    private final AiClientProperties aiClientProperties;

    /**
     * 한두 글자 입력은 Typecast(ssfm)가 억양을 지어내며 없는 말을 덧붙이는
     * 환각("가"→"가라고 읽어봐", "그"→"그 그" 류)이 간헐적으로 생긴다.
     * 낱자·음절 발음 학습은 정확성이 절대적이므로 Azure 합성으로 보낸다.
     */
    private static final int AZURE_ROUTE_MAX_LETTERS = 2;

    @Operation(summary = "베리 음성으로 한국어 MP3 생성")
    @PostMapping(produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(
            @CurrentStudentId Long studentId,
            @Valid @RequestBody TypecastTtsRequest request
    ) {
        // 실제 TTS는 베리 음성(Typecast)을 쓴다. 키 할당량 소진 시
        // TypecastKeyRing이 예비 키로 자동 전환한다. 목 모드와 한두 글자
        // 발음(환각 방지)만 AI 클라이언트 경로(Azure)로 우회한다.
        String text = request.text().trim();
        boolean shortUtterance =
                text.replaceAll("\\s+", "").length() <= AZURE_ROUTE_MAX_LETTERS;
        byte[] audio = aiClientProperties.ttsMocked() || shortUtterance
                ? aiClient.synthesizeSpeech(new SpeechSynthesisRequest(
                        UUID.randomUUID().toString(),
                        text,
                        null,
                        request.effectiveTempo()
                )).audio()
                : typecastTtsClient.synthesize(text, request.effectiveTempo());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }
}
