package com.iread.backend.gaze.repository;

import com.iread.backend.gaze.domain.GazeSessionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GazeSessionRepository extends JpaRepository<GazeSessionEntity, Long> {
    @EntityGraph(attributePaths = "student")
    Optional<GazeSessionEntity> findByIdAndStudentId(Long id, Long studentId);
}
