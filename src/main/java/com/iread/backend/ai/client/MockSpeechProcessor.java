package com.iread.backend.ai.client;

import com.iread.backend.ai.dto.req.SpeechSynthesisRequest;
import com.iread.backend.ai.dto.res.SpeechSynthesisResponse;
import com.iread.backend.ai.dto.res.SpeechTranscriptionResponse;
import org.springframework.stereotype.Component;

@Component
public class MockSpeechProcessor {

    public SpeechTranscriptionResponse transcribe(String requestId, String expectedText) {
        String transcript = expectedText == null || expectedText.isBlank()
                ? "친구를 따라간다"
                : expectedText;
        return new SpeechTranscriptionResponse(requestId, transcript, 1.0, 1_000);
    }

    public SpeechSynthesisResponse synthesize(SpeechSynthesisRequest request) {
        long durationMs = Math.max(
                1_000L,
                request.text().codePointCount(0, request.text().length()) * 250L
        );
        return new SpeechSynthesisResponse(
                new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0},
                durationMs
        );
    }
}
