package com.iread.backend.ai.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiClientPropertiesTest {

    @Test
    void 개별_음성_플래그가_없으면_mock_speech를_따른다() {
        AiClientProperties properties = properties(true, null, null, null);

        assertThat(properties.pronunciationMocked()).isTrue();
        assertThat(properties.transcribeMocked()).isTrue();
        assertThat(properties.ttsMocked()).isTrue();
    }

    @Test
    void 발음_평가만_실제_AI_서버로_전환할_수_있다() {
        AiClientProperties properties = properties(true, false, null, null);

        assertThat(properties.pronunciationMocked()).isFalse();
        assertThat(properties.transcribeMocked()).isTrue();
        assertThat(properties.ttsMocked()).isTrue();
    }

    @Test
    void 개별_플래그는_mock_speech가_꺼져_있어도_mock을_유지한다() {
        AiClientProperties properties = properties(false, null, true, true);

        assertThat(properties.pronunciationMocked()).isFalse();
        assertThat(properties.transcribeMocked()).isTrue();
        assertThat(properties.ttsMocked()).isTrue();
    }

    @Test
    void 실제_AI_연동은_공유_API_키가_필수다() {
        assertThatThrownBy(() -> new AiClientProperties(
                URI.create("http://localhost:8081"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                "",
                false,
                true,
                true,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ai.api-key");
    }

    private AiClientProperties properties(
            boolean mockSpeech,
            Boolean mockPronunciation,
            Boolean mockTranscribe,
            Boolean mockTts
    ) {
        return new AiClientProperties(
                URI.create("http://localhost:8081"),
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                "test-api-key",
                true,
                true,
                mockSpeech,
                mockPronunciation,
                mockTranscribe,
                mockTts
        );
    }
}
