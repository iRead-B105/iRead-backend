package com.iread.backend.gaze.repository;

import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;

public interface GazeSessionRepository extends JpaRepository<GazeSessionEntity, Long> {
    boolean existsByTrainingIdAndStatusAndDataIsNotNull(
            Long trainingId,
            GazeSessionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "student")
    @Query("""
            select session from GazeSessionEntity session
             where session.id = :id and session.student.id = :studentId
            """)
    Optional<GazeSessionEntity> findByIdAndStudentIdForUpdate(
            @Param("id") Long id,
            @Param("studentId") Long studentId
    );

    long countByStudentIdAndContentTypeAndStatusAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            Long studentId,
            GazeContentType contentType,
            GazeSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    );
}
