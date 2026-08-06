package com.iread.backend.auth.repository;

import com.iread.backend.auth.domain.AuthAudience;
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

    /** 같은 용도의 기존 로그인 세션을 밀어낸다(교수자 웹: 교사 단위). */
    @Modifying
    @Query("""
            update AuthRefreshSessionEntity session
               set session.revokedAt = :revokedAt
             where session.teacher.id = :teacherId
               and session.audience = :audience
               and session.revokedAt is null
            """)
    int revokeAllByTeacherIdAndAudience(
            @Param("teacherId") Long teacherId,
            @Param("audience") AuthAudience audience,
            @Param("revokedAt") Instant revokedAt
    );

    /** 같은 아동의 기존 학습 세션을 밀어낸다(다른 아동의 동시 학습은 허용). */
    @Modifying
    @Query("""
            update AuthRefreshSessionEntity session
               set session.revokedAt = :revokedAt
             where session.teacher.id = :teacherId
               and session.student.id = :studentId
               and session.audience = :audience
               and session.revokedAt is null
            """)
    int revokeAllByTeacherIdAndStudentIdAndAudience(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId,
            @Param("audience") AuthAudience audience,
            @Param("revokedAt") Instant revokedAt
    );

    /** 접근 토큰의 세션(sid)이 아직 유효한지 — 밀려난 기기 즉시 차단용. */
    boolean existsByIdAndRevokedAtIsNullAndExpiresAtAfter(Long id, Instant now);
}
