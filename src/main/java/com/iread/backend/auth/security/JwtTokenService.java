package com.iread.backend.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iread.backend.auth.config.AuthSettings;
import com.iread.backend.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtTokenService {

    public static final String ADMIN_AUDIENCE = "admin-app";
    public static final String LEARNING_AUDIENCE = "learning-app";
    public static final String BOOTSTRAP_AUDIENCE = "learning-bootstrap";

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HEADER = BASE64_URL_ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)
    );
    private static final Set<String> ALLOWED_AUDIENCES = Set.of(
            ADMIN_AUDIENCE,
            LEARNING_AUDIENCE,
            BOOTSTRAP_AUDIENCE
    );

    private final ObjectMapper objectMapper;
    private final AuthSettings settings;

    public JwtTokenService(ObjectMapper objectMapper, AuthSettings settings) {
        this.objectMapper = objectMapper;
        this.settings = settings;
    }

    public IssuedToken issueAdminAccessToken(Long teacherId) {
        return issue(teacherId, null, AuthRole.TEACHER, ADMIN_AUDIENCE, settings.accessTokenTtl());
    }

    public IssuedToken issueBootstrapToken(Long teacherId) {
        return issue(teacherId, null, AuthRole.TEACHER, BOOTSTRAP_AUDIENCE, settings.bootstrapTokenTtl());
    }

    public IssuedToken issueLearningAccessToken(Long teacherId, Long studentId) {
        return issue(teacherId, studentId, AuthRole.STUDENT, LEARNING_AUDIENCE, settings.accessTokenTtl());
    }

    public AuthPrincipal parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw invalidToken();
            }

            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw invalidToken();
            }

            JsonNode header = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                throw invalidToken();
            }

            JsonNode claims = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[1]));
            long expiresAt = claims.path("exp").asLong(0);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "인증 토큰이 만료되었습니다.");
            }

            String audience = claims.path("aud").asText();
            if (!ALLOWED_AUDIENCES.contains(audience)) {
                throw invalidToken();
            }

            Long teacherId = requiredLong(claims, "teacherId");
            JsonNode studentIdNode = claims.get("studentId");
            Long studentId = studentIdNode == null || studentIdNode.isNull() ? null : studentIdNode.asLong();
            AuthRole role = AuthRole.valueOf(claims.path("role").asText());
            String tokenId = claims.path("jti").asText();
            if (tokenId.isBlank()) {
                throw invalidToken();
            }
            return new AuthPrincipal(
                    teacherId,
                    studentId,
                    role,
                    audience,
                    tokenId,
                    Instant.ofEpochSecond(expiresAt)
            );
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private IssuedToken issue(
            Long teacherId,
            Long studentId,
            AuthRole role,
            String audience,
            Duration ttl
    ) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", studentId == null ? teacherId.toString() : studentId.toString());
        claims.put("teacherId", teacherId);
        if (studentId != null) {
            claims.put("studentId", studentId);
        }
        claims.put("role", role.name());
        claims.put("aud", audience);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        try {
            String payload = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String signingInput = HEADER + "." + payload;
            String signature = BASE64_URL_ENCODER.encodeToString(sign(signingInput));
            return new IssuedToken(signingInput + "." + signature, ttl.toSeconds());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("인증 토큰을 생성할 수 없습니다.", exception);
        }
    }

    private byte[] sign(String value) {
        byte[] secret = settings.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET은 32바이트 이상이어야 합니다.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("인증 토큰 서명을 생성할 수 없습니다.", exception);
        }
    }

    private Long requiredLong(JsonNode claims, String name) {
        JsonNode value = claims.get(name);
        if (value == null || !value.canConvertToLong()) {
            throw invalidToken();
        }
        return value.asLong();
    }

    private AuthException invalidToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 인증 토큰입니다.");
    }

    public record IssuedToken(String value, long expiresIn) {
    }
}
