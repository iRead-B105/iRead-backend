package com.iread.backend.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPrincipalTest {

    @Test
    void 역할과Audience가일치하는인증주체를허용한다() {
        assertThatCode(() -> new AuthPrincipal(
                1L,
                null,
                AuthRole.TEACHER,
                JwtTokenService.ADMIN_AUDIENCE,
                "admin-token",
                Instant.now().plusSeconds(60)
        )).doesNotThrowAnyException();

        assertThatCode(() -> new AuthPrincipal(
                1L,
                2L,
                AuthRole.STUDENT,
                JwtTokenService.LEARNING_AUDIENCE,
                "student-token",
                Instant.now().plusSeconds(60)
        )).doesNotThrowAnyException();
    }

    @Test
    void 관리자Audience의학생역할을거부한다() {
        assertThatThrownBy(() -> new AuthPrincipal(
                1L,
                2L,
                AuthRole.STUDENT,
                JwtTokenService.ADMIN_AUDIENCE,
                "invalid-token",
                Instant.now().plusSeconds(60)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역할과 audience");
    }

    @Test
    void 학생식별자가없는학습Audience를거부한다() {
        assertThatThrownBy(() -> new AuthPrincipal(
                1L,
                null,
                AuthRole.STUDENT,
                JwtTokenService.LEARNING_AUDIENCE,
                "invalid-token",
                Instant.now().plusSeconds(60)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역할과 audience");
    }
}
