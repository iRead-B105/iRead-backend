package com.iread.backend.training.repository;

import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DailyCurriculumRepository extends JpaRepository<DailyCurriculumEntity, Long> {
    @EntityGraph(attributePaths = {"trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    List<DailyCurriculumEntity> findAllByStudentIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    Optional<DailyCurriculumEntity> findByIdAndStudentId(Long id, Long studentId);

    @EntityGraph(attributePaths = {"trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    Optional<DailyCurriculumEntity> findByStudentIdAndStatus(Long studentId, DailyCurriculumStatus status);

    @EntityGraph(attributePaths = {"student", "trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    List<DailyCurriculumEntity> findAllByStatus(DailyCurriculumStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    @Query("select curriculum from DailyCurriculumEntity curriculum where curriculum.id = :curriculumId")
    Optional<DailyCurriculumEntity> findForGeneration(@Param("curriculumId") Long curriculumId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "trainings", "trainings.trainingTemplate", "trainings.trainingTemplate.curriculumUnit"})
    @Query("""
            select curriculum
            from DailyCurriculumEntity curriculum
            where curriculum.id = :curriculumId
              and curriculum.student.id = :studentId
            """)
    Optional<DailyCurriculumEntity> findForUpdate(
            @Param("curriculumId") Long curriculumId,
            @Param("studentId") Long studentId
    );
}
