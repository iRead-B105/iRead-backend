package com.iread.backend.exception;

public class ConflictException extends IllegalStateException {
    public ConflictException(String message) {
        super(message);
    }
}
