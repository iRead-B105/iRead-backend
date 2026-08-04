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

    @Operation(summary = "베리 음성으로 한국어 MP3 생성")
    @PostMapping(produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesize(
            @CurrentStudentId Long studentId,
            @Valid @RequestBody TypecastTtsRequest request
    ) {
        byte[] audio = aiClientProperties.ttsMocked()
                ? aiClient.synthesizeSpeech(new SpeechSynthesisRequest(
                        UUID.randomUUID().toString(),
                        request.text().trim(),
                        null
                )).audio()
                : typecastTtsClient.synthesize(request.text().trim(), request.effectiveTempo());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }
}
