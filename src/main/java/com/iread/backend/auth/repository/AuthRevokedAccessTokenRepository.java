package com.iread.backend.auth.repository;

import com.iread.backend.auth.domain.AuthRevokedAccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuthRevokedAccessTokenRepository extends JpaRepository<AuthRevokedAccessTokenEntity, String> {

    @Modifying
    @Query("delete from AuthRevokedAccessTokenEntity token where token.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
