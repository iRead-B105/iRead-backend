package com.iread.backend.gaze.repository;

import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GazeAnalysisResultRepository extends JpaRepository<GazeAnalysisResultEntity, Long> {
    boolean existsByGazeSessionId(Long gazeSessionId);

    @EntityGraph(attributePaths = "gazeSession")
    List<GazeAnalysisResultEntity> findAllByGazeSessionIdIn(List<Long> gazeSessionIds);

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

    @EntityGraph(attributePaths = {
            "gazeSession",
            "gazeSession.training",
            "gazeSession.test"
    })
    List<GazeAnalysisResultEntity>
    findAllByGazeSessionStudentIdAndGazeSessionContentTypeAndGazeSessionStartedAtGreaterThanEqualAndGazeSessionStartedAtLessThanOrderByCreatedAtAscIdAsc(
            Long studentId,
            GazeContentType contentType,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    );
}
