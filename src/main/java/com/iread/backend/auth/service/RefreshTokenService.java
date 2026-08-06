package com.iread.backend.auth.service;

import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.domain.AuthAudience;
import com.iread.backend.auth.domain.AuthRefreshSessionEntity;
import com.iread.backend.auth.exception.AuthException;
import com.iread.backend.auth.repository.AuthRefreshSessionRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.teacher.domain.TeacherEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthRefreshSessionRepository repository;
    private final AuthSettings settings;

    public RefreshTokenService(AuthRefreshSessionRepository repository, AuthSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    @Transactional
    public IssuedRefreshToken issue(
            TeacherEntity teacher,
            StudentEntity student,
            AuthAudience audience
    ) {
        // 단일 세션 정책(마지막 로그인 승리): 새 세션이 같은 범위의 기존 세션을 밀어낸다.
        // 교수자 웹(ADMIN)은 교사 단위, 학습(LEARNING)은 아동 단위로 적용해
        // 한 교사의 여러 아동이 각자 기기에서 동시에 학습하는 교실 시나리오는 유지한다.
        Instant now = Instant.now();
        if (student == null) {
            repository.revokeAllByTeacherIdAndAudience(teacher.getId(), audience, now);
        } else {
            repository.revokeAllByTeacherIdAndStudentIdAndAudience(
                    teacher.getId(),
                    student.getId(),
                    audience,
                    now
            );
        }

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = now.plus(settings.refreshTokenTtl());
        AuthRefreshSessionEntity session = new AuthRefreshSessionEntity(
                teacher,
                student,
                audience,
                hash(rawToken),
                expiresAt
        );
        repository.save(session);
        return new IssuedRefreshToken(rawToken, session);
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken, AuthAudience expectedAudience) {
        Instant now = Instant.now();
        AuthRefreshSessionEntity current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);
        if (current.isRevoked() || current.isExpired(now) || current.getAudience() != expectedAudience) {
            throw invalidRefreshToken();
        }

        current.revoke(now);
        return issue(current.getTeacher(), current.getStudent(), expectedAudience);
    }

    @Transactional
    public void revoke(String rawToken, AuthAudience expectedAudience) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHashForUpdate(hash(rawToken))
                .filter(session -> session.getAudience() == expectedAudience)
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAll(Long teacherId) {
        repository.revokeAllByTeacherId(teacherId, Instant.now());
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Refresh token 해시를 생성할 수 없습니다.", exception);
        }
    }

    private AuthException invalidRefreshToken() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "유효하지 않은 refresh token입니다."
        );
    }

    public record IssuedRefreshToken(
            String rawToken,
            AuthRefreshSessionEntity session
    ) {
    }
}
