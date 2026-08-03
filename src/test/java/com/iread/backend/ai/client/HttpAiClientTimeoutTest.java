package com.iread.backend.ai.client;

import com.iread.backend.ai.config.AiClientConfig;
import com.iread.backend.ai.config.AiClientProperties;
import com.iread.backend.ai.dto.req.GenerateTrainingRequest;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.global.audio.AudioUploadPolicy;
import com.iread.backend.global.audio.TemporaryAudioStorage;
import com.iread.backend.global.storage.FileStorage;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpAiClientTimeoutTest {

    @Test
    void 응답_제한시간을_초과한_상태변경_요청은_한번만_호출한다(@TempDir Path tempDir) throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/api/v1/trainings/generate", exchange -> {
            requestCount.incrementAndGet();
            try {
                Thread.sleep(500);
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            HttpAiClient client = client(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    Duration.ofSeconds(1),
                    Duration.ofMillis(100),
                    tempDir
            );

            assertThatThrownBy(() -> client.generateTraining(request()))
                    .isInstanceOf(AiClientException.class)
                    .hasMessage("AI 서버와 통신하는 데 실패했습니다.");
            assertThat(requestCount).hasValue(1);
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    @Test
    void 연결할_수_없는_AI_서버는_통신_예외로_변환한다(@TempDir Path tempDir) throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        HttpAiClient client = client(
                URI.create("http://127.0.0.1:" + unusedPort),
                Duration.ofMillis(100),
                Duration.ofSeconds(1),
                tempDir
        );

        assertThatThrownBy(() -> client.generateTraining(request()))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서버와 통신하는 데 실패했습니다.");
    }

    private HttpAiClient client(URI baseUrl, Duration connectTimeout, Duration readTimeout, Path tempDir) {
        AiClientProperties properties = new AiClientProperties(
                baseUrl,
                connectTimeout,
                readTimeout,
                "test-api-key",
                false,
                false,
                false,
                null,
                null,
                null
        );
        RestClient restClient = new AiClientConfig().aiRestClient(RestClient.builder(), properties);
        JsonMapper objectMapper = new JsonMapper();
        AudioUploadPolicy uploadPolicy = new AudioUploadPolicy(
                DataSize.ofMegabytes(20),
                "audio/webm,audio/wav,audio/mpeg,audio/mp4"
        );
        return new HttpAiClient(
                restClient,
                properties,
                new MockTrainingGenerator(objectMapper),
                new MockTrainingEvaluator(),
                new MockStoryGenerator(),
                new MockSpeechProcessor(),
                new TemporaryAudioStorage(tempDir.resolve("audio").toString(), uploadPolicy),
                org.mockito.Mockito.mock(FileStorage.class)
        );
    }

    private GenerateTrainingRequest request() {
        return new GenerateTrainingRequest(
                "timeout-request",
                10L,
                20L,
                30L,
                1,
                new JsonMapper().createObjectNode()
        );
    }
}
