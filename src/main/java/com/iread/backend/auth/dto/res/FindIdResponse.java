package com.iread.backend.auth.dto.res;

public record FindIdResponse(
        String maskedLoginId,
        String verificationStatus
) {
    public static FindIdResponse completed(String maskedLoginId) {
        return new FindIdResponse(maskedLoginId, "COMPLETED");
    }
}
