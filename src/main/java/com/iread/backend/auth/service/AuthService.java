package com.iread.backend.auth.service;

import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.dto.req.FindIdRequest;
import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.ResetPasswordRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.req.StudentLoginRequest;
import com.iread.backend.auth.dto.res.AdminLoginResponse;
import com.iread.backend.auth.dto.res.AppTeacherLoginResponse;
import com.iread.backend.auth.dto.res.FindIdResponse;
import com.iread.backend.auth.dto.res.PasswordResetResponse;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AuthSettings settings;

    public AuthService(
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            LoginAttemptService loginAttemptService,
            AuthSettings settings
    ) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.settings = settings;
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
                normalizeNullable(request.profileImage())
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
        JwtTokenService.IssuedToken accessToken = jwtTokenService.issueAdminAccessToken(teacher.getId());
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(teacher, null, AuthAudience.ADMIN);
        return new LoginResult<>(
                AdminLoginResponse.completed(teacher, accessToken.value(), accessToken.expiresIn()),
                refreshToken.rawToken()
        );
    }

    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        validateDemoVerificationCode(request.verificationCode());
        TeacherEntity teacher = teacherRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        "TEACHER_NOT_FOUND",
                        "계정 정보를 확인할 수 없습니다."
                ));
        teacher.updatePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(teacher.getId());
        return PasswordResetResponse.completed();
    }

    @Transactional(readOnly = true)
    public FindIdResponse findId(FindIdRequest request) {
        TeacherEntity teacher = teacherRepository.findByNameAndEmail(request.name(), request.email())
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        "TEACHER_NOT_FOUND",
                        "계정 정보를 확인할 수 없습니다."
                ));
        return FindIdResponse.completed(mask(teacher.getEmail()));
    }

    @Transactional
    public TokenResult refreshAdmin(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken rotated =
                refreshTokenService.rotate(rawRefreshToken, AuthAudience.ADMIN);
        JwtTokenService.IssuedToken accessToken =
                jwtTokenService.issueAdminAccessToken(rotated.session().getTeacher().getId());
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
        JwtTokenService.IssuedToken accessToken =
                jwtTokenService.issueLearningAccessToken(principal.id(), student.getId());
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(student.getTeacher(), student, AuthAudience.LEARNING);
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
                student.getId()
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

    private void validateDemoVerificationCode(String providedCode) {
        String configuredCode = settings.demoVerificationCode();
        if (configuredCode == null || configuredCode.isBlank()) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "DEMO_VERIFICATION_NOT_CONFIGURED",
                    "데모 비밀번호 재설정 코드가 설정되지 않았습니다."
            );
        }
        if (!MessageDigest.isEqual(
                configuredCode.getBytes(StandardCharsets.UTF_8),
                providedCode.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_CODE",
                    "인증 코드가 올바르지 않습니다."
            );
        }
    }

    private String mask(String email) {
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        String domain = atIndex > 0 ? email.substring(atIndex) : "";
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        }
        int visiblePrefix = Math.min(3, localPart.length() - 1);
        int maskLength = Math.max(1, localPart.length() - visiblePrefix);
        return localPart.substring(0, visiblePrefix)
                + "*".repeat(maskLength)
                + domain;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record LoginResult<T>(T response, String refreshToken) {
    }

    public record TokenResult(TokenRefreshResponse response, String refreshToken) {
    }
}
