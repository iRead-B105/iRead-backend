package com.iread.backend.typecast;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "typecast")
public record TypecastTtsProperties(
        URI baseUrl,
        String apiKey,
        String apiKey1,
        String apiKey2,
        String apiKey3,
        String apiKey4,
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

    /**
     * 줄 단위 개별 프로퍼티로 등록한 키들을 순서대로 모은다(중복 제거).
     * 예:
     * <pre>
     * typecast.api-key-1=key1
     * typecast.api-key-2=key2
     * typecast.api-key-3=key3
     * typecast.api-key-4=key4
     * </pre>
     * 하위호환용 단일 키(typecast.api-key)도 있으면 목록 끝에 합친다.
     */
    public List<String> resolvedApiKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String key : new String[] { apiKey1, apiKey2, apiKey3, apiKey4, apiKey }) {
            if (key != null && !key.isBlank()) {
                keys.add(key.trim());
            }
        }
        return List.copyOf(new ArrayList<>(keys));
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Objects.requireNonNull(duration, propertyName + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
