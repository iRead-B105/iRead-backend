package com.iread.backend.auth.service;

import com.iread.backend.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        String key = hash(email);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        if (attempt.windowStartedAt().plus(WINDOW).isBefore(Instant.now())) {
            attempts.remove(key, attempt);
            return;
        }
        if (attempt.failures() >= MAX_FAILURES) {
            throw new AuthException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LOGIN_RATE_LIMITED",
                    "잠시 후 다시 로그인해 주세요."
            );
        }
    }

    public void recordFailure(String email) {
        String key = hash(email);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.windowStartedAt().plus(WINDOW).isBefore(now)) {
                return new Attempt(1, now);
            }
            return new Attempt(current.failures() + 1, current.windowStartedAt());
        });
    }

    public void clear(String email) {
        attempts.remove(hash(email));
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("로그인 시도 식별자를 생성할 수 없습니다.", exception);
        }
    }

    private record Attempt(int failures, Instant windowStartedAt) {
    }
}
