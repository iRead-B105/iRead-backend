package com.iread.backend.auth.security;

public record AuthPrincipal(
        Long id,
        Long studentId,
        AuthRole role,
        String audience,
        String tokenId,
        java.time.Instant expiresAt,
        /** 접근 토큰이 묶인 로그인 세션(sid). 부트스트랩 토큰 등 세션 없는 토큰은 null. */
        Long sessionId
) {
    /** 세션 바인딩이 없는 주체(부트스트랩·테스트 픽스처)용. */
    public AuthPrincipal(
            Long id,
            Long studentId,
            AuthRole role,
            String audience,
            String tokenId,
            java.time.Instant expiresAt
    ) {
        this(id, studentId, role, audience, tokenId, expiresAt, null);
    }

    public AuthPrincipal {
        if (id == null || role == null || audience == null || tokenId == null || expiresAt == null) {
            throw new IllegalArgumentException("인증 주체의 필수 정보가 없습니다.");
        }

        boolean teacherPrincipal = role == AuthRole.TEACHER
                && studentId == null
                && (JwtTokenService.ADMIN_AUDIENCE.equals(audience)
                || JwtTokenService.BOOTSTRAP_AUDIENCE.equals(audience));
        boolean studentPrincipal = role == AuthRole.STUDENT
                && studentId != null
                && JwtTokenService.LEARNING_AUDIENCE.equals(audience);
        if (!teacherPrincipal && !studentPrincipal) {
            throw new IllegalArgumentException("인증 역할과 audience 조합이 올바르지 않습니다.");
        }
    }
}
