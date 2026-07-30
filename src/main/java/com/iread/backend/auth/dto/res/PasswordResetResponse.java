package com.iread.backend.auth.dto.res;

public record PasswordResetResponse(
        String verificationStatus,
        String resetStatus
) {
    public static PasswordResetResponse completed() {
        return new PasswordResetResponse("COMPLETED", "COMPLETED");
    }
}
