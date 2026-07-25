package com.iread.backend.auth.service;

import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.domain.AuthRefreshSessionEntity;
import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.ResetPasswordRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.req.StudentLoginRequest;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.AuthRole;
import com.iread.backend.auth.security.JwtTokenService;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock StudentRepository studentRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenService jwtTokenService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AccessTokenRevocationService accessTokenRevocationService;
    @Mock LoginAttemptService loginAttemptService;

    private AuthService authService;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        AuthSettings settings = new AuthSettings(
                "test-secret-that-is-at-least-32-bytes-long",
                Duration.ofMinutes(15),
                Duration.ofMinutes(5),
                Duration.ofDays(14),
                false,
                "123456"
        );
        authService = new AuthService(
                teacherRepository,
                studentRepository,
                passwordEncoder,
                jwtTokenService,
                refreshTokenService,
                accessTokenRevocationService,
                loginAttemptService,
                settings
        );
        teacher = new TeacherEntity(
                "teacher01",
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
    void signsUpWithSeparateLoginIdAndHashedPassword() {
        SignUpRequest request = new SignUpRequest(
                "teacher02",
                "teacher02@example.com",
                "password123",
                "새교사",
                "기관",
                null
        );
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(teacherRepository.save(any(TeacherEntity.class))).thenAnswer(invocation -> {
            TeacherEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        var response = authService.signUp(request);

        assertThat(response.teacherId()).isEqualTo("20");
        assertThat(response.loginId()).isEqualTo("teacher02");
        assertThat(response.email()).isEqualTo("teacher02@example.com");
        verify(teacherRepository).existsByLoginId("teacher02");
        verify(teacherRepository).existsByEmail("teacher02@example.com");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void adminLoginIssuesAudienceTokenAndRefreshSession() {
        LoginRequest request = new LoginRequest("teacher01", "password123");
        when(teacherRepository.findByLoginId("teacher01")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenService.issueAdminAccessToken(10L))
                .thenReturn(new JwtTokenService.IssuedToken("access-token", 900));
        AuthRefreshSessionEntity session = new AuthRefreshSessionEntity(
                teacher,
                null,
                AuthAudience.ADMIN,
                "hash",
                Instant.now().plusSeconds(60)
        );
        when(refreshTokenService.issue(teacher, null, AuthAudience.ADMIN))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-token", session));

        AuthService.LoginResult<?> result = authService.adminLogin(request);

        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response()).extracting("accessToken").isEqualTo("access-token");
        verify(loginAttemptService).clear("teacher01");
    }

    @Test
    void studentLoginRejectsStudentOutsideTeacherOwnership() {
        AuthPrincipal principal = new AuthPrincipal(
                10L,
                null,
                AuthRole.TEACHER,
                JwtTokenService.BOOTSTRAP_AUDIENCE,
                "token-id",
                Instant.now().plusSeconds(60)
        );
        when(studentRepository.findByIdAndTeacherId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.studentLogin(principal, new StudentLoginRequest("99")))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("STUDENT_NOT_FOUND");
        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void passwordResetRejectsWrongDemoCode() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "teacher01",
                "teacher@example.com",
                "wrong-code",
                "new-password"
        );

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("INVALID_VERIFICATION_CODE");
        verify(teacherRepository, never()).findByLoginIdAndEmail(any(), any());
    }

    @Test
    void passwordResetUpdatesHashAndRevokesAllSessions() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "teacher01",
                "teacher@example.com",
                "123456",
                "new-password"
        );
        when(teacherRepository.findByLoginIdAndEmail("teacher01", "teacher@example.com"))
                .thenReturn(Optional.of(teacher));
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

        var response = authService.resetPassword(request);

        assertThat(response.resetStatus()).isEqualTo("COMPLETED");
        assertThat(teacher.getPassword()).isEqualTo("new-encoded-password");
        verify(refreshTokenService).revokeAll(10L);
    }
}
