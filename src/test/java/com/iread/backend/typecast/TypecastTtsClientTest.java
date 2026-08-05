package com.iread.backend.typecast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TypecastTtsClientTest {
    private MockRestServiceServer server;
    private TypecastTtsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.typecast.ai");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TypecastTtsClient(
                builder.build(),
                properties("test-key", "tc_beri"),
                new TypecastKeyRing(List.of("test-key"))
        );
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
                properties("", "tc_beri"),
                new TypecastKeyRing(List.of())
        );

        assertThatThrownBy(() -> unconfigured.synthesize("안녕하세요", 1.0))
                .isInstanceOf(TypecastTtsException.class)
                .hasMessageContaining("API 키");
    }

    @Test
    void rotatesToNextKeyAndRetriesAfterSecondQuotaFailure() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.typecast.ai");
        MockRestServiceServer rotationServer = MockRestServiceServer.bindTo(builder).build();
        TypecastKeyRing ring = new TypecastKeyRing(List.of("key1", "key2"));
        TypecastTtsClient rotatingClient = new TypecastTtsClient(
                builder.build(),
                properties("key1", "tc_beri"),
                ring
        );

        // 기대 요청은 실행 전에 전부 등록해야 한다(MockRestServiceServer 제약).
        // 1번째 호출: key1로 403 → 연속 1회, 전환 없이 업스트림 예외
        rotationServer.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(header("X-API-KEY", "key1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        // 2번째 호출: key1로 403(연속 2회) → key2로 전환 후 같은 요청을 재시도해 성공
        rotationServer.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(header("X-API-KEY", "key1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        rotationServer.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(header("X-API-KEY", "key2"))
                .andRespond(withSuccess(new byte[]{9}, MediaType.parseMediaType("audio/mpeg")));

        assertThatThrownBy(() -> rotatingClient.synthesize("안녕", 1.0))
                .isInstanceOf(TypecastTtsException.class)
                .hasMessageContaining("403");
        assertThat(rotatingClient.synthesize("안녕", 1.0)).containsExactly(9);
        assertThat(ring.activeKey()).isEqualTo("key2");
        rotationServer.verify();
    }

    @Test
    void invalidKeyRotatesImmediatelyAndRetries() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.typecast.ai");
        MockRestServiceServer rotationServer = MockRestServiceServer.bindTo(builder).build();
        TypecastKeyRing ring = new TypecastKeyRing(List.of("dead-key", "live-key"));
        TypecastTtsClient rotatingClient = new TypecastTtsClient(
                builder.build(),
                properties("dead-key", "tc_beri"),
                ring
        );

        rotationServer.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(header("X-API-KEY", "dead-key"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        rotationServer.expect(once(), requestTo("https://api.typecast.ai/v1/text-to-speech"))
                .andExpect(header("X-API-KEY", "live-key"))
                .andRespond(withSuccess(new byte[]{7}, MediaType.parseMediaType("audio/mpeg")));

        assertThat(rotatingClient.synthesize("안녕", 1.0)).containsExactly(7);
        assertThat(ring.activeKey()).isEqualTo("live-key");
        rotationServer.verify();
    }

    private TypecastTtsProperties properties(String apiKey, String voiceId) {
        return new TypecastTtsProperties(
                URI.create("https://api.typecast.ai"),
                apiKey,
                null,
                null,
                null,
                null,
                voiceId,
                "Beri",
                "ssfm-v30",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30)
        );
    }
}
