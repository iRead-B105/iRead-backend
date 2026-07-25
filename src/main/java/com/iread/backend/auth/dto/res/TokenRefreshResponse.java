package com.iread.backend.auth.dto.res;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static TokenRefreshResponse bearer(String accessToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, "Bearer", expiresIn);
    }
}
