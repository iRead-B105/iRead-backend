package com.iread.backend.auth.service;

import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.domain.AuthRefreshSessionEntity;
import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.req.StudentLoginRequest;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.AuthRole;
import com.iread.backend.auth.security.JwtTokenService;
import com.iread.backend.student.domain.StudentEntity;
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

import java.time.Instant;
import java.util.List;
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
    @Mock LoginAttemptService loginAttemptService;

    private AuthService authService;
    private TeacherEntity teacher;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                teacherRepository,
                studentRepository,
                passwordEncoder,
                jwtTokenService,
                refreshTokenService,
                loginAttemptService
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
    void signsUpWithUniqueEmailAndHashedPassword() {
        SignUpRequest request = new SignUpRequest(
                "teacher02@example.com",
                "password123",
                "새교사",
                "기관"
        );
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(teacherRepository.save(any(TeacherEntity.class))).thenAnswer(invocation -> {
            TeacherEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        var response = authService.signUp(request);

        assertThat(response.teacherId()).isEqualTo("20");
        assertThat(response.email()).isEqualTo("teacher02@example.com");
        verify(teacherRepository).existsByEmail("teacher02@example.com");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void adminLoginIssuesAudienceTokenAndRefreshSession() {
        LoginRequest request = new LoginRequest("teacher@example.com", "password123");
        when(teacherRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenService.issueAdminAccessToken(10L, null))
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
        verify(loginAttemptService).clear("teacher@example.com");
    }

    @Test
    void appTeacherLoginReturnsLinkedStudentProfilesForSelection() {
        LoginRequest request = new LoginRequest("teacher@example.com", "password123");
        StudentEntity student = StudentEntity.builder()
                .teacher(teacher)
                .name("김아동")
                .imageUrl("https://cdn.example.com/students/20.png")
                .build();
        ReflectionTestUtils.setField(student, "id", 20L);
        when(teacherRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenService.issueBootstrapToken(10L))
                .thenReturn(new JwtTokenService.IssuedToken("bootstrap-token", 300));
        when(studentRepository.findAllByTeacherIdOrderByIdAsc(10L)).thenReturn(List.of(student));

        var response = authService.appTeacherLogin(request);

        assertThat(response.linkedStudents()).singleElement().satisfies(linkedStudent -> {
            assertThat(linkedStudent.studentId()).isEqualTo("20");
            assertThat(linkedStudent.name()).isEqualTo("김아동");
            assertThat(linkedStudent.profileImage())
                    .isEqualTo("https://cdn.example.com/students/20.png");
        });
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
    void adminLogoutRevokesRefreshSession() {
        AuthPrincipal principal = new AuthPrincipal(
                10L,
                null,
                AuthRole.TEACHER,
                JwtTokenService.ADMIN_AUDIENCE,
                "token-id",
                Instant.now().plusSeconds(60)
        );

        authService.logoutAdmin(principal, "refresh-token");

        verify(refreshTokenService).revoke("refresh-token", AuthAudience.ADMIN);
    }

    @Test
    void learningLogoutRevokesRefreshSession() {
        AuthPrincipal principal = new AuthPrincipal(
                10L,
                20L,
                AuthRole.STUDENT,
                JwtTokenService.LEARNING_AUDIENCE,
                "token-id",
                Instant.now().plusSeconds(60)
        );

        authService.logoutLearning(principal, "refresh-token");

        verify(refreshTokenService).revoke("refresh-token", AuthAudience.LEARNING);
    }
}
