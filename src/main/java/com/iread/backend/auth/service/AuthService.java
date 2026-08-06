package com.iread.backend.auth.service;

import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.req.StudentLoginRequest;
import com.iread.backend.auth.dto.res.AdminLoginResponse;
import com.iread.backend.auth.dto.res.AppTeacherLoginResponse;
import com.iread.backend.auth.dto.res.SignUpResponse;
import com.iread.backend.auth.dto.res.StudentLoginResponse;
import com.iread.backend.auth.dto.res.TokenRefreshResponse;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.security.AuthRole;
import com.iread.backend.auth.security.JwtTokenService;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            LoginAttemptService loginAttemptService
    ) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (teacherRepository.existsByEmail(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }

        TeacherEntity teacher = new TeacherEntity(
                request.email().trim(),
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                request.organization().trim(),
                null,
                null
        );
        return SignUpResponse.completed(teacherRepository.save(teacher));
    }

    @Transactional(readOnly = true)
    public TeacherEntity authenticate(LoginRequest request) {
        loginAttemptService.checkAllowed(request.email());
        TeacherEntity teacher = teacherRepository.findByEmail(request.email())
                .orElse(null);
        if (teacher == null || !passwordEncoder.matches(request.password(), teacher.getPassword())) {
            loginAttemptService.recordFailure(request.email());
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }
        loginAttemptService.clear(request.email());
        return teacher;
    }

    @Transactional
    public LoginResult<AdminLoginResponse> adminLogin(LoginRequest request) {
        TeacherEntity teacher = authenticate(request);
        // 세션을 먼저 발급(같은 범위 기존 세션 밀어내기 포함)하고 접근 토큰을 세션에 묶는다.
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(teacher, null, AuthAudience.ADMIN);
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueAdminAccessToken(
                teacher.getId(),
                refreshToken.session().getId()
        );
        return new LoginResult<>(
                AdminLoginResponse.completed(teacher, accessToken.value(), accessToken.expiresIn()),
                refreshToken.rawToken()
        );
    }

    @Transactional
    public TokenResult refreshAdmin(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated =
                refreshTokenService.rotate(rawRefreshToken, AuthAudience.ADMIN);
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueAdminAccessToken(
                rotated.session().getTeacher().getId(),
                rotated.session().getId()
        );
        return new TokenResult(
                TokenRefreshResponse.bearer(accessToken.value(), accessToken.expiresIn()),
                rotated.rawToken()
        );
    }

    @Transactional(readOnly = true)
    public AppTeacherLoginResponse appTeacherLogin(LoginRequest request) {
        TeacherEntity teacher = authenticate(request);
        JwtTokenService.IssuedToken bootstrapToken = jwtTokenService.issueBootstrapToken(teacher.getId());
        List<StudentEntity> students = studentRepository.findAllByTeacherIdOrderByIdAsc(teacher.getId());
        return AppTeacherLoginResponse.selectionRequired(
                teacher,
                bootstrapToken.value(),
                bootstrapToken.expiresIn(),
                students
        );
    }

    @Transactional
    public LoginResult<StudentLoginResponse> studentLogin(
            AuthPrincipal principal,
            StudentLoginRequest request
    ) {
        if (principal == null
                || principal.role() != AuthRole.TEACHER
                || !JwtTokenService.BOOTSTRAP_AUDIENCE.equals(principal.audience())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "INVALID_TOKEN_AUDIENCE", "아동 선택 권한이 없습니다.");
        }

        Long studentId;
        try {
            studentId = Long.valueOf(request.studentId());
        } catch (NumberFormatException exception) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_STUDENT_ID", "아동 ID 형식이 올바르지 않습니다.");
        }
        StudentEntity student = studentRepository.findByIdAndTeacherId(studentId, principal.id())
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        "STUDENT_NOT_FOUND",
                        "연결된 아동을 찾을 수 없습니다."
                ));
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(student.getTeacher(), student, AuthAudience.LEARNING);
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueLearningAccessToken(
                principal.id(),
                student.getId(),
                refreshToken.session().getId()
        );
        return new LoginResult<>(
                StudentLoginResponse.completed(
                        student.getId().toString(),
                        accessToken.value(),
                        accessToken.expiresIn()
                ),
                refreshToken.rawToken()
        );
    }

    @Transactional
    public TokenResult refreshLearning(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated =
                refreshTokenService.rotate(rawRefreshToken, AuthAudience.LEARNING);
        StudentEntity student = rotated.session().getStudent();
        if (student == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 refresh token입니다.");
        }
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueLearningAccessToken(
                rotated.session().getTeacher().getId(),
                student.getId(),
                rotated.session().getId()
        );
        return new TokenResult(
                TokenRefreshResponse.bearer(accessToken.value(), accessToken.expiresIn()),
                rotated.rawToken()
        );
    }

    public void logoutAdmin(AuthPrincipal principal, String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken, AuthAudience.ADMIN);
    }

    public void logoutLearning(AuthPrincipal principal, String rawRefreshToken) {
        if (principal == null
                || (!JwtTokenService.LEARNING_AUDIENCE.equals(principal.audience())
                && !JwtTokenService.BOOTSTRAP_AUDIENCE.equals(principal.audience()))) {
            throw new AuthException(HttpStatus.FORBIDDEN, "INVALID_TOKEN_AUDIENCE", "학습 앱 로그아웃 권한이 없습니다.");
        }
        refreshTokenService.revoke(rawRefreshToken, AuthAudience.LEARNING);
    }

    public record LoginResult<T>(T response, String refreshToken) {
    }

    public record TokenResult(TokenRefreshResponse response, String refreshToken) {
    }
}
