package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingDataRepository extends JpaRepository<TrainingDataEntity, Long> {
    Optional<TrainingDataEntity> findByTrainingId(Long trainingId);
    void deleteByTrainingId(Long trainingId);
}
