package com.iread.backend.auth.controller;

import com.iread.backend.auth.dto.req.ConfirmPasswordResetRequest;
import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.PasswordResetLinkRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.req.StudentLoginRequest;
import com.iread.backend.auth.dto.res.AdminLoginResponse;
import com.iread.backend.auth.dto.res.AppTeacherLoginResponse;
import com.iread.backend.auth.dto.res.PasswordResetLinkResponse;
import com.iread.backend.auth.dto.res.PasswordResetResponse;
import com.iread.backend.auth.dto.res.SignUpResponse;
import com.iread.backend.auth.dto.res.StudentLoginResponse;
import com.iread.backend.auth.dto.res.TokenRefreshResponse;
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.service.AuthCookieService;
import com.iread.backend.auth.service.AuthService;
import com.iread.backend.auth.service.PasswordResetService;
import com.iread.backend.auth.service.StudentProfileImageService;
import com.iread.backend.global.storage.LoadedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "관리자 앱과 학습 앱 JWT 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final PasswordResetService passwordResetService;
    private final StudentProfileImageService studentProfileImageService;

    public AuthController(
            AuthService authService,
            AuthCookieService cookieService,
            PasswordResetService passwordResetService,
            StudentProfileImageService studentProfileImageService
    ) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.passwordResetService = passwordResetService;
        this.studentProfileImageService = studentProfileImageService;
    }

    @Operation(summary = "관리자 앱 로그인")
    @PostMapping("/admin/login")
    public ResponseEntity<AdminLoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult<AdminLoginResponse> result = authService.adminLogin(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.adminRefresh(result.refreshToken()).toString())
                .body(result.response());
    }

    @Operation(summary = "관리자 앱 로그아웃")
    @PostMapping("/admin/logout")
    public ResponseEntity<Void> adminLogout(
            @AuthenticationPrincipal AuthPrincipal principal,
            @CookieValue(value = AuthCookieService.ADMIN_REFRESH_COOKIE, required = false) String refreshToken
    ) {
        authService.logoutAdmin(principal, refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearAdminRefresh().toString())
                .build();
    }

    @Operation(summary = "교수자 비밀번호 재설정 링크 요청")
    @PostMapping("/admin/password-reset/request")
    public ResponseEntity<PasswordResetLinkResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetLinkRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.accepted().body(
                passwordResetService.requestReset(
                        request.email(),
                        servletRequest.getRemoteAddr()
                )
        );
    }

    @Operation(summary = "교수자 비밀번호 재설정 확정")
    @PostMapping("/admin/password-reset/confirm")
    public PasswordResetResponse confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetRequest request,
            HttpServletRequest servletRequest
    ) {
        return passwordResetService.confirmReset(
                request.token(),
                request.newPassword(),
                servletRequest.getRemoteAddr()
        );
    }

    @Operation(summary = "관리자 앱 토큰 갱신")
    @PostMapping("/admin/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshAdmin(
            @CookieValue(AuthCookieService.ADMIN_REFRESH_COOKIE) String refreshToken
    ) {
        AuthService.TokenResult result = authService.refreshAdmin(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.adminRefresh(result.refreshToken()).toString())
                .body(result.response());
    }

    @Operation(summary = "교수자 회원가입")
    @PostMapping("/admin/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    @Operation(summary = "학습 앱 로그아웃")
    @PostMapping("/app/logout")
    public ResponseEntity<Void> appLogout(
            @AuthenticationPrincipal AuthPrincipal principal,
            @CookieValue(value = AuthCookieService.LEARNING_REFRESH_COOKIE, required = false) String refreshToken
    ) {
        authService.logoutLearning(principal, refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearLearningRefresh().toString())
                .build();
    }

    @Operation(summary = "학습 앱 토큰 갱신")
    @PostMapping("/app/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshLearning(
            @CookieValue(AuthCookieService.LEARNING_REFRESH_COOKIE) String refreshToken
    ) {
        AuthService.TokenResult result = authService.refreshLearning(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.learningRefresh(result.refreshToken()).toString())
                .body(result.response());
    }

    @Operation(summary = "학습 앱 아동 로그인")
    @PostMapping("/app/student-login")
    public ResponseEntity<StudentLoginResponse> studentLogin(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody StudentLoginRequest request
    ) {
        AuthService.LoginResult<StudentLoginResponse> result = authService.studentLogin(principal, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.learningRefresh(result.refreshToken()).toString())
                .body(result.response());
    }

    @Operation(summary = "학습 앱 연결 아동 프로필 이미지 조회")
    @GetMapping("/app/students/{studentId}/profile-image")
    public ResponseEntity<byte[]> studentProfileImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String studentId
    ) {
        LoadedFile image = studentProfileImageService.load(principal, studentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(image.content());
    }

    @Operation(summary = "학습 앱 교수자 로그인")
    @PostMapping("/app/teacher-login")
    public AppTeacherLoginResponse appTeacherLogin(@Valid @RequestBody LoginRequest request) {
        return authService.appTeacherLogin(request);
    }
}
