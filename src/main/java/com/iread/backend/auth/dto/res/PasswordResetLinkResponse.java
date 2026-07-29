package com.iread.backend.auth.dto.res;

public record PasswordResetLinkResponse(
        String requestStatus
) {
    public static PasswordResetLinkResponse accepted() {
        return new PasswordResetLinkResponse("ACCEPTED");
    }
}
