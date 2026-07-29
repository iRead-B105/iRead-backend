package com.iread.backend.exception;

public class ResourceNotFoundException extends IllegalArgumentException {
    private final String code;

    public ResourceNotFoundException(String message) {
        this("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
