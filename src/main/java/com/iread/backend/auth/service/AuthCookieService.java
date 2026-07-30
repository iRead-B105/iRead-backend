package com.iread.backend.auth.service;

import com.iread.backend.auth.config.AuthSettings;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String ADMIN_REFRESH_COOKIE = "admin_refresh_token";
    public static final String LEARNING_REFRESH_COOKIE = "learning_refresh_token";

    private final AuthSettings settings;

    public AuthCookieService(AuthSettings settings) {
        this.settings = settings;
    }

    public ResponseCookie adminRefresh(String value) {
        return create(ADMIN_REFRESH_COOKIE, value, "/api/auth/admin", settings.refreshTokenTtl());
    }

    public ResponseCookie learningRefresh(String value) {
        return create(LEARNING_REFRESH_COOKIE, value, "/api/auth/app", settings.refreshTokenTtl());
    }

    public ResponseCookie clearAdminRefresh() {
        return create(ADMIN_REFRESH_COOKIE, "", "/api/auth/admin", Duration.ZERO);
    }

    public ResponseCookie clearLearningRefresh() {
        return create(LEARNING_REFRESH_COOKIE, "", "/api/auth/app", Duration.ZERO);
    }

    private ResponseCookie create(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(settings.secureCookie())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
