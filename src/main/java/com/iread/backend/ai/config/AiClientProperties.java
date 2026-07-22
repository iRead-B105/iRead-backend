package com.iread.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "ai")
public record AiClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String apiKey
) {
    public AiClientProperties {
        Objects.requireNonNull(baseUrl, "ai.base-url은 필수입니다.");
        requirePositive(connectTimeout, "ai.connect-timeout");
        requirePositive(readTimeout, "ai.read-timeout");
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Objects.requireNonNull(duration, propertyName + "은 필수입니다.");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + "은 0보다 커야 합니다.");
        }
    }
}
