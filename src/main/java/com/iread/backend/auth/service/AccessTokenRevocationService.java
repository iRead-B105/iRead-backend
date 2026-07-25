package com.iread.backend.auth.service;

import com.iread.backend.auth.domain.AuthRevokedAccessTokenEntity;
import com.iread.backend.auth.repository.AuthRevokedAccessTokenRepository;
import com.iread.backend.auth.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AccessTokenRevocationService {

    private final AuthRevokedAccessTokenRepository repository;

    public AccessTokenRevocationService(AuthRevokedAccessTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return repository.existsById(tokenId);
    }

    @Transactional
    public void revoke(AuthPrincipal principal) {
        if (principal == null || principal.tokenId() == null || principal.expiresAt() == null) {
            return;
        }
        Instant now = Instant.now();
        repository.deleteExpired(now);
        if (principal.expiresAt().isAfter(now) && !repository.existsById(principal.tokenId())) {
            repository.save(new AuthRevokedAccessTokenEntity(
                    principal.tokenId(),
                    principal.expiresAt(),
                    now
            ));
        }
    }
}
