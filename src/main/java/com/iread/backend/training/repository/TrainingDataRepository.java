package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingDataRepository extends JpaRepository<TrainingDataEntity, Long> {
    Optional<TrainingDataEntity> findByTrainingId(Long trainingId);
    List<TrainingDataEntity> findAllByTrainingIdIn(Collection<Long> trainingIds);
    void deleteByTrainingId(Long trainingId);
}
