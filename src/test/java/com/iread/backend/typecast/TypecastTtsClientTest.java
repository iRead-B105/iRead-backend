package com.iread.backend.typecast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TypecastTtsClientTest {
    private MockRestServiceServer server;
    private TypecastTtsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.typecast.ai")
                .defaultHeader("X-API-KEY", "test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TypecastTtsClient(builder.build(), properties("test-key", "tc_beri"));
    }

    @Test
    void synthesizesKoreanMp3WithBeriVoice() {
        server.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "test-key"))
                .andExpect(content().json("""
                        {
                          "voice_id":"tc_beri",
                          "text":"안녕하세요",
                          "model":"ssfm-v30",
                          "language":"kor",
                          "output":{
                            "volume":100,
                            "audio_pitch":0,
                            "audio_tempo":0.9,
                            "audio_format":"mp3"
                          }
                        }
                        """))
                .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.parseMediaType("audio/mpeg")));

        assertThat(client.synthesize("안녕하세요", 0.9)).containsExactly(1, 2, 3);
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingTypecast() {
        TypecastTtsClient unconfigured = new TypecastTtsClient(
                RestClient.create("https://api.typecast.ai"),
                properties("", "tc_beri")
        );

        assertThatThrownBy(() -> unconfigured.synthesize("안녕하세요", 1.0))
                .isInstanceOf(TypecastTtsException.class)
                .hasMessageContaining("API 키");
    }

    private TypecastTtsProperties properties(String apiKey, String voiceId) {
        return new TypecastTtsProperties(
                URI.create("https://api.typecast.ai"),
                apiKey,
                voiceId,
                "Beri",
                "ssfm-v30",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30)
        );
    }
}
