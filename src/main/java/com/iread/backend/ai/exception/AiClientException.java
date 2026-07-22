package com.iread.backend.ai.exception;

public class AiClientException extends RuntimeException {

    private final Integer statusCode;

    public AiClientException(String message) {
        this(message, null, null);
    }

    public AiClientException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public AiClientException(String message, int statusCode) {
        this(message, null, statusCode);
    }

    private AiClientException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
