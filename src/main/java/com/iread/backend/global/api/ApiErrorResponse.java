package com.iread.backend.global.api;

public record ApiErrorResponse(
        ErrorDetail error
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ErrorDetail(code, message));
    }

    public record ErrorDetail(
            String code,
            String message
    ) {
    }
}
