package com.iread.backend.auth.service;

import com.iread.backend.auth.config.PasswordResetSettings;
import com.iread.backend.auth.domain.PasswordResetTokenEntity;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.repository.PasswordResetTokenRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordResetMailSender mailSender;
    @Mock PasswordResetRateLimiter rateLimiter;

    private PasswordResetService service;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                teacherRepository,
                tokenRepository,
                passwordEncoder,
                refreshTokenService,
                mailSender,
                rateLimiter,
                new PasswordResetSettings(
                        Duration.ofMinutes(10),
                        "http://localhost:5173/reset-password",
                        "no-reply@iread.local",
                        5,
                        Duration.ofMinutes(15)
                )
        );
        teacher = new TeacherEntity(
                "teacher@example.com",
                "encoded-password",
                "교사",
                "기관",
                null,
                null
        );
        ReflectionTestUtils.setField(teacher, "id", 10L);
    }

    @Test
    void unknownEmailReturnsSameAcceptedResponseWithoutToken() {
        when(teacherRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        var response = service.requestReset(" Missing@example.com ", "127.0.0.1");

        assertThat(response.requestStatus()).isEqualTo("ACCEPTED");
        verify(rateLimiter).checkAndRecord("missing@example.com", "127.0.0.1");
        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).sendResetLink(anyString(), anyString());
    }

    @Test
    void requestStoresOnlyHashAndInvalidatesPreviousLink() {
        when(teacherRepository.findByEmail("teacher@example.com"))
                .thenReturn(Optional.of(teacher));

        var response = service.requestReset("teacher@example.com", "127.0.0.1");

        assertThat(response.requestStatus()).isEqualTo("ACCEPTED");
        verify(tokenRepository).invalidateActiveByTeacherId(any(), any());
        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendResetLink(
                org.mockito.ArgumentMatchers.eq("teacher@example.com"),
                linkCaptor.capture()
        );

        String rawToken = linkCaptor.getValue().substring(
                linkCaptor.getValue().indexOf("token=") + "token=".length()
        );
        assertThat(rawToken).isNotBlank();
        assertThat(tokenCaptor.getValue().getTokenHash())
                .hasSize(64)
                .doesNotContain(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt())
                .isAfter(Instant.now().plus(Duration.ofMinutes(9)));
    }

    @Test
    void mailFailureStillReturnsNeutralAcceptedResponse() {
        when(teacherRepository.findByEmail("teacher@example.com"))
                .thenReturn(Optional.of(teacher));
        doThrow(new IllegalStateException("smtp unavailable"))
                .when(mailSender)
                .sendResetLink(anyString(), anyString());

        var response = service.requestReset("teacher@example.com", "127.0.0.1");

        assertThat(response.requestStatus()).isEqualTo("ACCEPTED");
        verify(tokenRepository).save(any());
    }

    @Test
    void validTokenChangesPasswordUsesTokenAndRevokesSessions() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                teacher,
                "hash",
                Instant.now().plusSeconds(60)
        );
        when(tokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-encoded-password");

        var response = service.confirmReset("raw-token", "new-password", "127.0.0.1");

        assertThat(response.resetStatus()).isEqualTo("COMPLETED");
        assertThat(teacher.getPassword()).isEqualTo("new-encoded-password");
        assertThat(token.isUsed()).isTrue();
        verify(refreshTokenService).revokeAll(10L);
    }

    @Test
    void usedTokenIsRejected() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                teacher,
                "hash",
                Instant.now().plusSeconds(60)
        );
        token.use(Instant.now());
        when(tokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(
                () -> service.confirmReset("raw-token", "new-password", "127.0.0.1")
        )
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_RESET_TOKEN_INVALID");
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    void expiredTokenIsRejected() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                teacher,
                "hash",
                Instant.now().minusSeconds(1)
        );
        when(tokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(
                () -> service.confirmReset("raw-token", "new-password", "127.0.0.1")
        )
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("PASSWORD_RESET_TOKEN_EXPIRED");
        verify(refreshTokenService, never()).revokeAll(any());
    }
}
