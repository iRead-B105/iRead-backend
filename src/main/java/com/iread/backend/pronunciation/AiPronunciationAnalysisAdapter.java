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
        // 자모 낱자 문항(ㅏ, ㅆ 등)은 발화 음절로 바꿔야 Azure가 정렬할 수 있다.
        // PronunciationWordAligner.normalize도 같은 변환으로 결과를 원문과 맞춘다.
        return aiClient.analyzePronunciation(new PronunciationAnalysisRequest(
                request.requestId(),
                JamoPronunciations.toSpokenText(request.expectedText()),
                request.originalFilename(),
                request.contentType(),
                request.audio()
        ));
    }
}
