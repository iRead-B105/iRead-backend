package com.iread.backend.auth.service;

import com.iread.backend.auth.config.PasswordResetSettings;
import com.iread.backend.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordResetRateLimiter {

    private final PasswordResetSettings settings;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public PasswordResetRateLimiter(PasswordResetSettings settings) {
        this.settings = settings;
    }

    public void checkAndRecord(String email, String clientAddress) {
        Instant now = Instant.now();
        record("request-account:" + email.trim().toLowerCase(Locale.ROOT), now);
        record("request-ip:" + normalizeAddress(clientAddress), now);
    }

    public void checkAndRecordConfirmation(String rawToken, String clientAddress) {
        Instant now = Instant.now();
        record("confirm-token:" + rawToken, now);
        record("confirm-ip:" + normalizeAddress(clientAddress), now);
    }

    private void record(String rawKey, Instant now) {
        String key = hash(rawKey);
        Attempt updated = attempts.compute(key, (ignored, current) -> {
            if (current == null
                    || !current.windowStartedAt().plus(settings.rateLimitWindow()).isAfter(now)) {
                return new Attempt(1, now);
            }
            return new Attempt(current.requests() + 1, current.windowStartedAt());
        });
        if (updated.requests() > settings.maxRequests()) {
            throw new AuthException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "PASSWORD_RESET_RATE_LIMITED",
                    "잠시 후 다시 비밀번호 재설정을 요청해 주세요."
            );
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("비밀번호 재설정 요청 식별자를 생성할 수 없습니다.", exception);
        }
    }

    private String normalizeAddress(String clientAddress) {
        return clientAddress == null ? "unknown" : clientAddress;
    }

    private record Attempt(int requests, Instant windowStartedAt) {
    }
}
