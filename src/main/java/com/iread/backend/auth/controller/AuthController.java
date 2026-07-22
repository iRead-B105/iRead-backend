package com.iread.backend.auth.controller;

import com.iread.backend.auth.dto.req.LoginRequest;
import com.iread.backend.auth.dto.req.SignUpRequest;
import com.iread.backend.auth.dto.res.TeacherAuthResponse;
import com.iread.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "인증", description = "교사 회원가입 및 세션 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "교사 회원가입")
    @PostMapping(value = "/sign-up", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TeacherAuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @Operation(summary = "프로필 이미지와 함께 교사 회원가입")
    @PostMapping(value = "/sign-up", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeacherAuthResponse signUpWithImage(
            @Valid @RequestPart("request") SignUpRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return authService.signUp(request, image);
    }

    @Operation(summary = "교사 로그인 및 세션 생성")
    @PostMapping("/login")
    public TeacherAuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(request, httpRequest);
    }

    @Operation(summary = "교사 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "현재 로그인한 교사 조회")
    @GetMapping("/me")
    public TeacherAuthResponse me(HttpServletRequest request) {
        return authService.getLoginTeacher(request);
    }
}
