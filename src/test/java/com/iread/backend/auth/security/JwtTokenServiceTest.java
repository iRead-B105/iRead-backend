package com.iread.backend.auth.security;

import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

    @Test
    void issuesAndValidatesAdminAccessToken() {
        JwtTokenService service = service(Duration.ofMinutes(15));

        JwtTokenService.IssuedToken token = service.issueAdminAccessToken(10L);
        AuthPrincipal principal = service.parseAndValidate(token.value());

        assertThat(token.expiresIn()).isEqualTo(900);
        assertThat(principal.id()).isEqualTo(10L);
        assertThat(principal.studentId()).isNull();
        assertThat(principal.role()).isEqualTo(AuthRole.TEACHER);
        assertThat(principal.audience()).isEqualTo(JwtTokenService.ADMIN_AUDIENCE);
        assertThat(principal.tokenId()).isNotBlank();
    }

    @Test
    void learningTokenContainsTeacherAndStudentOwnership() {
        JwtTokenService service = service(Duration.ofMinutes(15));

        AuthPrincipal principal = service.parseAndValidate(
                service.issueLearningAccessToken(10L, 20L).value()
        );

        assertThat(principal.id()).isEqualTo(10L);
        assertThat(principal.studentId()).isEqualTo(20L);
        assertThat(principal.role()).isEqualTo(AuthRole.STUDENT);
        assertThat(principal.audience()).isEqualTo(JwtTokenService.LEARNING_AUDIENCE);
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService service = service(Duration.ofMinutes(15));
        String token = service.issueAdminAccessToken(10L).value();
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> service.parseAndValidate(tampered))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("INVALID_TOKEN");
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService service = service(Duration.ofSeconds(-1));
        String token = service.issueAdminAccessToken(10L).value();

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("TOKEN_EXPIRED");
    }

    private JwtTokenService service(Duration accessTtl) {
        AuthSettings settings = new AuthSettings(
                SECRET,
                accessTtl,
                Duration.ofMinutes(5),
                Duration.ofDays(14),
                false,
                "123456"
        );
        return new JwtTokenService(JsonMapper.builder().build(), settings);
    }
}
