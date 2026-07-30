package com.iread.backend.gaze.repository;

import com.iread.backend.gaze.domain.WordAttemptLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordAttemptLogRepository extends JpaRepository<WordAttemptLogEntity, Long> {
    List<WordAttemptLogEntity> findAllByGazeAnalysisResultIdOrderByWordIndexAsc(Long gazeAnalysisResultId);
}
