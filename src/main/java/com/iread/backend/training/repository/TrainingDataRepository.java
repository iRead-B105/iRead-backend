package com.iread.backend.training.repository;

import com.iread.backend.training.domain.TrainingDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingDataRepository extends JpaRepository<TrainingDataEntity, Long> {
    Optional<TrainingDataEntity> findByTrainingId(Long trainingId);

    /** 생성된 문항 데이터가 해당 학생 소유이면서 지정 삽화 파일을 참조하는지. */
    @org.springframework.data.jpa.repository.Query("""
            select count(data) > 0
              from TrainingDataEntity data
             where data.training.dailyCurriculum.student.id = :studentId
               and data.generatedData like concat('%', :fileName, '%')
            """)
    boolean existsByStudentIdAndImageFileName(
            @org.springframework.data.repository.query.Param("studentId") Long studentId,
            @org.springframework.data.repository.query.Param("fileName") String fileName
    );
    List<TrainingDataEntity> findAllByTrainingIdIn(Collection<Long> trainingIds);
    void deleteByTrainingId(Long trainingId);
}
