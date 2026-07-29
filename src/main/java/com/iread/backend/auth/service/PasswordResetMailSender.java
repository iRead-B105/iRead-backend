package com.iread.backend.auth.service;

public interface PasswordResetMailSender {
    void sendResetLink(String recipient, String resetLink);
}
