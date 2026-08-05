package com.iread.backend.auth.domain;

public enum AuthAudience {
    ADMIN("admin-app"),
    LEARNING("learning-app");

    private final String tokenValue;

    AuthAudience(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String tokenValue() {
        return tokenValue;
    }
}
