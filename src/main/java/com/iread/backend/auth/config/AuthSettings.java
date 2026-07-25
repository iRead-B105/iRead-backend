package com.iread.backend.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public record AuthSettings(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration bootstrapTokenTtl,
        Duration refreshTokenTtl,
        boolean secureCookie,
        String demoVerificationCode
) {
    public AuthSettings(
            @Value("${auth.jwt.secret}") String jwtSecret,
            @Value("${auth.jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${auth.jwt.bootstrap-token-ttl}") Duration bootstrapTokenTtl,
            @Value("${auth.refresh-token-ttl}") Duration refreshTokenTtl,
            @Value("${auth.cookie.secure}") boolean secureCookie,
            @Value("${auth.demo.verification-code}") String demoVerificationCode
    ) {
        this.jwtSecret = jwtSecret;
        this.accessTokenTtl = accessTokenTtl;
        this.bootstrapTokenTtl = bootstrapTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.secureCookie = secureCookie;
        this.demoVerificationCode = demoVerificationCode;
    }
}
