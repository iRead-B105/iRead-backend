package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingTemplateEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainingTemplateRepository extends JpaRepository<TrainingTemplateEntity, Long> {
    @EntityGraph(attributePaths = "curriculumUnit")
    List<TrainingTemplateEntity> findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc();

    @EntityGraph(attributePaths = "curriculumUnit")
    @Query("""
            select template
            from TrainingTemplateEntity template
            where template.id between :firstId and :lastId
            order by template.curriculumUnit.sequenceNo, template.sequenceNo
            """)
    List<TrainingTemplateEntity> findCanonicalCatalog(
            @Param("firstId") long firstId,
            @Param("lastId") long lastId
    );
}
