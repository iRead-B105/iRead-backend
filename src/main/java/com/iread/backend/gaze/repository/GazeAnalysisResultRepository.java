package com.iread.backend.gaze.repository;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GazeAnalysisResultRepository extends JpaRepository<GazeAnalysisResultEntity, Long> {
    boolean existsByGazeSessionId(Long gazeSessionId);

    Optional<GazeAnalysisResultEntity> findByIdAndGazeSessionStudentTeacherId(
            Long id,
            Long teacherId
    );
    Optional<GazeAnalysisResultEntity> findFirstByGazeSessionStudentIdAndGazeSessionTestIdOrderByCreatedAtDesc(
            Long studentId, Long testId
    );

    Optional<GazeAnalysisResultEntity> findFirstByGazeSessionStudentIdAndGazeSessionTrainingIdOrderByCreatedAtDesc(
            Long studentId, Long trainingId
    );
}
