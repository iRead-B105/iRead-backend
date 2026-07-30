package com.iread.backend.global.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ApiErrorResponse(
        ErrorDetail error
) {
    public static ApiErrorResponse of(String code, String message) {
        return of(code, message, null);
    }

    public static ApiErrorResponse of(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return new ApiErrorResponse(new ErrorDetail(code, message, details));
    }

    public record ErrorDetail(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> details
    ) {
    }
}
