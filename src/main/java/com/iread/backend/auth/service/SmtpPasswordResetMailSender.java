package com.iread.backend.auth.service;

import com.iread.backend.auth.config.PasswordResetSettings;
import com.iread.backend.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

    private final JavaMailSender mailSender;
    private final PasswordResetSettings settings;

    public SmtpPasswordResetMailSender(
            JavaMailSender mailSender,
            PasswordResetSettings settings
    ) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Override
    public void sendResetLink(String recipient, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.from());
        message.setTo(recipient);
        message.setSubject("[iRead] 비밀번호 재설정");
        message.setText("""
                iRead 교수자 계정의 비밀번호 재설정 요청을 받았습니다.

                아래 링크는 10분 동안 한 번만 사용할 수 있습니다.
                %s

                요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(resetLink));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new AuthException(
                    HttpStatus.BAD_GATEWAY,
                    "PASSWORD_RESET_EMAIL_FAILED",
                    "비밀번호 재설정 메일을 발송하지 못했습니다."
            );
        }
    }
}
