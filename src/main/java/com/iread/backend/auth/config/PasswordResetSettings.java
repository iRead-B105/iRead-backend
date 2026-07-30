package com.iread.backend.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public record PasswordResetSettings(
        Duration tokenTtl,
        String frontendUrl,
        String from,
        int maxRequests,
        Duration rateLimitWindow
) {
    public PasswordResetSettings(
            @Value("${auth.password-reset.token-ttl:10m}") Duration tokenTtl,
            @Value("${auth.password-reset.frontend-url:http://localhost:5173/reset-password}") String frontendUrl,
            @Value("${auth.password-reset.from:no-reply@iread.local}") String from,
            @Value("${auth.password-reset.rate-limit.max-requests:5}") int maxRequests,
            @Value("${auth.password-reset.rate-limit.window:15m}") Duration rateLimitWindow
    ) {
        if (tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalArgumentException("비밀번호 재설정 토큰 만료 시간은 양수여야 합니다.");
        }
        if (maxRequests < 1) {
            throw new IllegalArgumentException("비밀번호 재설정 요청 한도는 1 이상이어야 합니다.");
        }
        this.tokenTtl = tokenTtl;
        this.frontendUrl = frontendUrl.replaceAll("[?&]+$", "");
        this.from = from;
        this.maxRequests = maxRequests;
        this.rateLimitWindow = rateLimitWindow;
    }
}
