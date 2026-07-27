package com.iread.backend.auth.dto.res;

public record FindIdResponse(
        String maskedEmail,
        String verificationStatus
) {
    public static FindIdResponse completed(String maskedEmail) {
        return new FindIdResponse(maskedEmail, "COMPLETED");
    }
}
