package com.iread.backend.ai.config;

import com.iread.backend.global.audio.AudioUploadPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiClientConfigurationIntegrationTest {

    @Autowired
    AiClientProperties aiClientProperties;

    @Autowired
    AudioUploadPolicy audioUploadPolicy;

    @Test
    void 권장_AI_제한시간과_음성_업로드_정책을_바인딩한다() {
        assertThat(aiClientProperties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(aiClientProperties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(audioUploadPolicy.maxSizeBytes()).isEqualTo(20L * 1024 * 1024);
        assertThat(audioUploadPolicy.allowedContentTypes()).containsExactlyInAnyOrder(
                "audio/webm",
                "audio/wav",
                "audio/mpeg",
                "audio/mp4"
        );
    }
}
