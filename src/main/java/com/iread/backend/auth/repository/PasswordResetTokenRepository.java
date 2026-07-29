package com.iread.backend.auth.repository;

import com.iread.backend.auth.domain.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
              from PasswordResetTokenEntity token
              join fetch token.teacher
             where token.tokenHash = :tokenHash
            """)
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
            update PasswordResetTokenEntity token
               set token.usedAt = :usedAt
             where token.teacher.id = :teacherId
               and token.usedAt is null
            """)
    int invalidateActiveByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("usedAt") Instant usedAt
    );

    long deleteByExpiresAtBefore(Instant threshold);
}
