package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingTemplateEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingTemplateRepository extends JpaRepository<TrainingTemplateEntity, Long> {
    @EntityGraph(attributePaths = "curriculumUnit")
    List<TrainingTemplateEntity> findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc();
}
