package com.iread.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import org.springframework.util.StringUtils;

/**
 * {@code ai.mock-speech}는 음성 3종(발음 평가·STT·TTS)의 기본값이고
 * {@code ai.mock-pronunciation} · {@code ai.mock-transcribe} · {@code ai.mock-tts}로
 * 기능별로 덮어쓸 수 있다. AI server가 발음 평가만 구현한 상태에서 발음 평가만
 * 실제 Azure로 전환하고 스토리 STT·TTS는 mock으로 유지하기 위한 구분이다.
 * 개별 플래그를 지정하지 않으면 {@code null}이며 {@code ai.mock-speech}를 따른다.
 */
@ConfigurationProperties(prefix = "ai")
public record AiClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String apiKey,
        boolean mockGenerate,
        boolean mockEvaluate,
        boolean mockSpeech,
        Boolean mockPronunciation,
        Boolean mockTranscribe,
        Boolean mockTts
) {
    public AiClientProperties {
        Objects.requireNonNull(baseUrl, "ai.base-url은 필수입니다.");
        requirePositive(connectTimeout, "ai.connect-timeout");
        requirePositive(readTimeout, "ai.read-timeout");
        boolean realSpeech = !(mockPronunciation == null ? mockSpeech : mockPronunciation)
                || !(mockTranscribe == null ? mockSpeech : mockTranscribe)
                || !(mockTts == null ? mockSpeech : mockTts);
        if ((!mockGenerate || !mockEvaluate || realSpeech) && !StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException(
                    "AI 실제 연동을 사용하려면 ai.api-key가 필요합니다."
            );
        }
    }

    public boolean pronunciationMocked() {
        return mockPronunciation == null ? mockSpeech : mockPronunciation;
    }

    public boolean transcribeMocked() {
        return mockTranscribe == null ? mockSpeech : mockTranscribe;
    }

    public boolean ttsMocked() {
        return mockTts == null ? mockSpeech : mockTts;
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Objects.requireNonNull(duration, propertyName + "은 필수입니다.");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + "은 0보다 커야 합니다.");
        }
    }
}
