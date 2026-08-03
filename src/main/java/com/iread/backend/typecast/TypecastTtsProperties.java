package com.iread.backend.typecast;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "typecast")
public record TypecastTtsProperties(
        URI baseUrl,
        String apiKey,
        String voiceId,
        String voiceName,
        String model,
        Duration connectTimeout,
        Duration readTimeout
) {
    public TypecastTtsProperties {
        Objects.requireNonNull(baseUrl, "typecast.base-url is required");
        Objects.requireNonNull(model, "typecast.model is required");
        requirePositive(connectTimeout, "typecast.connect-timeout");
        requirePositive(readTimeout, "typecast.read-timeout");
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Objects.requireNonNull(duration, propertyName + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
