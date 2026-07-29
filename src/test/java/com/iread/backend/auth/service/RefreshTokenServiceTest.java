package com.iread.backend.auth.service;

import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.domain.AuthRefreshSessionEntity;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.repository.AuthRefreshSessionRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    AuthRefreshSessionRepository repository;

    private RefreshTokenService service;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        AuthSettings settings = new AuthSettings(
                "test-secret-that-is-at-least-32-bytes-long",
                Duration.ofMinutes(15),
                Duration.ofMinutes(5),
                Duration.ofDays(14),
                false
        );
        service = new RefreshTokenService(repository, settings);
        teacher = new TeacherEntity(
                "teacher@example.com",
                "encoded-password",
                "교사",
                "기관",
                null,
                null
        );
    }

    @Test
    void storesOnlyHashWhenIssuingRefreshToken() {
        RefreshTokenService.IssuedRefreshToken issued =
                service.issue(teacher, null, AuthAudience.ADMIN);

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.session().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(issued.rawToken());
        verify(repository).save(issued.session());
    }

    @Test
    void rotationRevokesCurrentSessionAndIssuesNewToken() {
        String currentRawToken = "current-refresh-token";
        AuthRefreshSessionEntity current = new AuthRefreshSessionEntity(
                teacher,
                null,
                AuthAudience.ADMIN,
                "ignored-current-hash",
                Instant.now().plusSeconds(60)
        );
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(current));

        RefreshTokenService.IssuedRefreshToken rotated =
                service.rotate(currentRawToken, AuthAudience.ADMIN);

        assertThat(current.isRevoked()).isTrue();
        assertThat(rotated.rawToken()).isNotEqualTo(currentRawToken);
        assertThat(rotated.session().getAudience()).isEqualTo(AuthAudience.ADMIN);
        verify(repository).save(rotated.session());
    }

    @Test
    void rejectsRefreshTokenForDifferentAudience() {
        AuthRefreshSessionEntity current = new AuthRefreshSessionEntity(
                teacher,
                null,
                AuthAudience.LEARNING,
                "ignored-current-hash",
                Instant.now().plusSeconds(60)
        );
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.rotate("raw-token", AuthAudience.ADMIN))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }
}
