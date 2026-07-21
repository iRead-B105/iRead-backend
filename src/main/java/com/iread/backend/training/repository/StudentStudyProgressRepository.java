package com.iread.backend.training.repository;

import com.iread.backend.training.domain.StudentStudyProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentStudyProgressRepository extends JpaRepository<StudentStudyProgressEntity, Long> {
    List<StudentStudyProgressEntity> findAllByStudentId(Long studentId);
}
