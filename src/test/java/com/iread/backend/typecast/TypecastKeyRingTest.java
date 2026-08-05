package com.iread.backend.typecast;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TypecastKeyRingTest {

    @Test
    void rotatesToNextKeyAfterTwoConsecutiveQuotaFailures() {
        TypecastKeyRing ring = new TypecastKeyRing(List.of("key1", "key2", "key3"));

        assertThat(ring.activeKey()).isEqualTo("key1");
        assertThat(ring.recordQuotaFailure()).isFalse();
        assertThat(ring.activeKey()).isEqualTo("key1");
        assertThat(ring.recordQuotaFailure()).isTrue();
        assertThat(ring.activeKey()).isEqualTo("key2");
    }

    @Test
    void successResetsTheConsecutiveFailureCounter() {
        TypecastKeyRing ring = new TypecastKeyRing(List.of("key1", "key2"));

        ring.recordQuotaFailure();
        ring.recordSuccess();
        assertThat(ring.recordQuotaFailure()).isFalse();
        assertThat(ring.activeKey()).isEqualTo("key1");
    }

    @Test
    void wrapsAroundToTheFirstKeyAfterTheLast() {
        TypecastKeyRing ring = new TypecastKeyRing(List.of("key1", "key2"));

        ring.recordQuotaFailure();
        ring.recordQuotaFailure();
        assertThat(ring.activeKey()).isEqualTo("key2");
        ring.recordQuotaFailure();
        ring.recordQuotaFailure();
        assertThat(ring.activeKey()).isEqualTo("key1");
    }

    @Test
    void invalidKeyRotatesImmediately() {
        TypecastKeyRing ring = new TypecastKeyRing(List.of("key1", "key2"));

        assertThat(ring.recordInvalidKey()).isTrue();
        assertThat(ring.activeKey()).isEqualTo("key2");
    }

    @Test
    void singleKeyNeverRotates() {
        TypecastKeyRing ring = new TypecastKeyRing(List.of("only"));

        assertThat(ring.recordQuotaFailure()).isFalse();
        assertThat(ring.recordQuotaFailure()).isFalse();
        assertThat(ring.recordInvalidKey()).isFalse();
        assertThat(ring.activeKey()).isEqualTo("only");
    }

    @Test
    void collectsLinePerKeyPropertiesInOrderWithoutDuplicates() {
        TypecastTtsProperties properties = new TypecastTtsProperties(
                URI.create("https://api.typecast.ai"),
                "legacy",
                "key1",
                "key2",
                "",
                null,
                "voice",
                "Beri",
                "ssfm-v30",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30)
        );

        assertThat(properties.resolvedApiKeys())
                .containsExactly("key1", "key2", "legacy");
    }
}
