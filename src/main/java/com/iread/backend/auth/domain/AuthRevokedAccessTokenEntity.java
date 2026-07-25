package com.iread.backend.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "auth_revoked_access_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRevokedAccessTokenEntity {

    @Id
    @Column(name = "token_id", length = 36)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    public AuthRevokedAccessTokenEntity(String tokenId, Instant expiresAt, Instant revokedAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }
}
