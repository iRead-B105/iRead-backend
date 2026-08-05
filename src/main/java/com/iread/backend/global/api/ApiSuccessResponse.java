package com.iread.backend.global.api;

public record ApiSuccessResponse(
        boolean success,
        Object data
) {
    public static ApiSuccessResponse of(Object data) {
        return new ApiSuccessResponse(true, data);
    }
}
