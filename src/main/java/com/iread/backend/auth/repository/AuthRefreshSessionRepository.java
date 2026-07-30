package com.iread.backend.auth.repository;

import com.iread.backend.auth.domain.AuthRefreshSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthRefreshSessionRepository extends JpaRepository<AuthRefreshSessionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
              from AuthRefreshSessionEntity session
              join fetch session.teacher
              left join fetch session.student
             where session.tokenHash = :tokenHash
            """)
    Optional<AuthRefreshSessionEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update AuthRefreshSessionEntity session
               set session.revokedAt = :revokedAt
             where session.teacher.id = :teacherId
               and session.revokedAt is null
            """)
    int revokeAllByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("revokedAt") Instant revokedAt
    );
}
