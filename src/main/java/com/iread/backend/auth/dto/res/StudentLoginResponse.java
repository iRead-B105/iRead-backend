package com.iread.backend.auth.dto.res;

public record StudentLoginResponse(
        String studentId,
        String accessToken,
        String tokenType,
        long expiresIn,
        String loginStatus
) {
    public static StudentLoginResponse completed(String studentId, String accessToken, long expiresIn) {
        return new StudentLoginResponse(studentId, accessToken, "Bearer", expiresIn, "COMPLETED");
    }
}
