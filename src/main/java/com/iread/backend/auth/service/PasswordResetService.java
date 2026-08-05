package com.iread.backend.auth.service;

import com.iread.backend.auth.config.PasswordResetSettings;
import com.iread.backend.auth.domain.PasswordResetTokenEntity;
import com.iread.backend.auth.dto.res.PasswordResetLinkResponse;
import com.iread.backend.auth.dto.res.PasswordResetResponse;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.repository.PasswordResetTokenRepository;
import com.iread.backend.teacher.domain.TeacherEntity;
import com.iread.backend.teacher.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Duration EXPIRED_TOKEN_RETENTION = Duration.ofDays(1);
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final TeacherRepository teacherRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetMailSender mailSender;
    private final PasswordResetRateLimiter rateLimiter;
    private final PasswordResetSettings settings;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            TeacherRepository teacherRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            PasswordResetMailSender mailSender,
            PasswordResetRateLimiter rateLimiter,
            PasswordResetSettings settings
    ) {
        this.teacherRepository = teacherRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
        this.settings = settings;
    }

    @Transactional
    public PasswordResetLinkResponse requestReset(String email, String clientAddress) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        rateLimiter.checkAndRecord(normalizedEmail, clientAddress);

        teacherRepository.findByEmail(normalizedEmail).ifPresent(teacher -> issue(teacher));
        return PasswordResetLinkResponse.accepted();
    }

    @Transactional
    public PasswordResetResponse confirmReset(
            String rawToken,
            String newPassword,
            String clientAddress
    ) {
        rateLimiter.checkAndRecordConfirmation(rawToken, clientAddress);
        Instant now = Instant.now();
        PasswordResetTokenEntity token = tokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::invalidToken);
        if (token.isUsed()) {
            throw invalidToken();
        }
        if (token.isExpired(now)) {
            token.use(now);
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_RESET_TOKEN_EXPIRED",
                    "비밀번호 재설정 링크가 만료되었습니다."
            );
        }

        TeacherEntity teacher = token.getTeacher();
        teacher.updatePassword(passwordEncoder.encode(newPassword));
        token.use(now);
        refreshTokenService.revokeAll(teacher.getId());
        return PasswordResetResponse.completed();
    }

    @Scheduled(cron = "0 20 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(
                Instant.now().minus(EXPIRED_TOKEN_RETENTION)
        );
    }

    private void issue(TeacherEntity teacher) {
        Instant now = Instant.now();
        tokenRepository.invalidateActiveByTeacherId(teacher.getId(), now);

        String rawToken = generateToken();
        tokenRepository.save(new PasswordResetTokenEntity(
                teacher,
                hash(rawToken),
                now.plus(settings.tokenTtl())
        ));
        try {
            mailSender.sendResetLink(teacher.getEmail(), resetLink(rawToken));
        } catch (RuntimeException exception) {
            // 요청 결과로 계정 존재 여부가 드러나지 않도록 발송 실패도 동일하게 접수 처리한다.
            log.warn("Password reset email delivery failed.");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resetLink(String rawToken) {
        String separator = settings.frontendUrl().contains("?") ? "&" : "?";
        return settings.frontendUrl() + separator + "token=" + rawToken;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("비밀번호 재설정 토큰 해시를 생성할 수 없습니다.", exception);
        }
    }

    private AuthException invalidToken() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_RESET_TOKEN_INVALID",
                "유효하지 않은 비밀번호 재설정 링크입니다."
        );
    }
}
