package com.iread.backend.pronunciation;

import com.iread.backend.ai.client.AiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPronunciationAnalysisAdapter implements PronunciationAnalysisAdapter {

    private final AiClient aiClient;

    @Override
    public PronunciationAnalysisResult analyze(PronunciationAnalysisRequest request) {
        return aiClient.analyzePronunciation(request);
    }
}
