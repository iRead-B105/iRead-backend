package com.iread.backend.auth.service;

import com.iread.backend.auth.config.PasswordResetSettings;
import com.iread.backend.auth.exception.AuthException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetRateLimiterTest {

    @Test
    void limitsBothAccountAndClientAddress() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(
                new PasswordResetSettings(
                        Duration.ofMinutes(10),
                        "http://localhost:5173/reset-password",
                        "no-reply@iread.local",
                        1,
                        Duration.ofMinutes(15)
                )
        );
        limiter.checkAndRecord("teacher@example.com", "127.0.0.1");

        assertThatThrownBy(
                () -> limiter.checkAndRecord("teacher@example.com", "127.0.0.2")
        )
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_RESET_RATE_LIMITED");
    }

    @Test
    void limitsConfirmationByTokenAndClientAddress() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(
                new PasswordResetSettings(
                        Duration.ofMinutes(10),
                        "http://localhost:5173/reset-password",
                        "no-reply@iread.local",
                        1,
                        Duration.ofMinutes(15)
                )
        );
        limiter.checkAndRecordConfirmation("reset-token", "127.0.0.1");

        assertThatThrownBy(
                () -> limiter.checkAndRecordConfirmation("reset-token", "127.0.0.2")
        )
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_RESET_RATE_LIMITED");
    }
}
