package com.iread.backend.auth.security;

public record AuthPrincipal(
        Long id,
        Long studentId,
        AuthRole role,
        String audience,
        String tokenId,
        java.time.Instant expiresAt
) {
}
