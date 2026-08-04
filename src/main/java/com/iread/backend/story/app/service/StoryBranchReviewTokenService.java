package com.iread.backend.story.app.service;

import com.iread.backend.auth.config.AuthSettings;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StoryBranchReviewTokenService {

    private static final long TOKEN_TTL_SECONDS = 600;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public StoryBranchReviewTokenService(ObjectMapper objectMapper, AuthSettings settings) {
        this.objectMapper = objectMapper;
        this.secret = settings.jwtSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long storyId, Long lineId, String transcript, String policyVersion) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("typ", "story-branch-review");
        claims.put("storyId", storyId);
        claims.put("lineId", lineId);
        claims.put("transcriptHash", hash(transcript));
        claims.put("policyVersion", policyVersion);
        claims.put("exp", Instant.now().plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond());
        try {
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            return payload + "." + ENCODER.encodeToString(sign(payload));
        } catch (JacksonException exception) {
            throw new IllegalStateException("분기 검토 토큰을 생성할 수 없습니다.", exception);
        }
    }

    public void verify(String token, Long storyId, Long lineId, String transcript) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2
                    || !MessageDigest.isEqual(sign(parts[0]), DECODER.decode(parts[1]))) {
                throw invalidToken();
            }
            JsonNode claims = objectMapper.readTree(DECODER.decode(parts[0]));
            if (!"story-branch-review".equals(claims.path("typ").asText())
                    || claims.path("storyId").asLong(-1) != storyId
                    || claims.path("lineId").asLong(-1) != lineId
                    || claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()
                    || !MessageDigest.isEqual(
                            claims.path("transcriptHash").asText().getBytes(StandardCharsets.UTF_8),
                            hash(transcript).getBytes(StandardCharsets.UTF_8)
                    )) {
                throw invalidToken();
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private byte[] sign(String payload) {
        if (secret.length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET은 32바이트 이상이어야 합니다.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("분기 검토 토큰 서명을 생성할 수 없습니다.", exception);
        }
    }

    private String hash(String transcript) {
        try {
            return ENCODER.encodeToString(
                    MessageDigest.getInstance("SHA-256")
                            .digest(transcript.strip().getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("분기 원문 해시를 생성할 수 없습니다.", exception);
        }
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("유효하지 않거나 만료된 분기 검토 토큰입니다.");
    }
}
