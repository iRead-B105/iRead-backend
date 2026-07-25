package com.iread.backend.auth.controller;

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
import com.iread.backend.auth.security.AuthPrincipal;
import com.iread.backend.auth.service.AuthCookieService;
import com.iread.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
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

    public AuthController(AuthService authService, AuthCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "교수자 아이디 찾기")
    @PostMapping("/admin/find-id")
    public FindIdResponse findId(@Valid @RequestBody FindIdRequest request) {
        return authService.findId(request);
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

    @Operation(summary = "교수자 비밀번호 재설정")
    @PostMapping("/admin/password-reset")
    public PasswordResetResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
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

    @Operation(summary = "학습 앱 교수자 로그인")
    @PostMapping("/app/teacher-login")
    public AppTeacherLoginResponse appTeacherLogin(@Valid @RequestBody LoginRequest request) {
        return authService.appTeacherLogin(request);
    }
}
