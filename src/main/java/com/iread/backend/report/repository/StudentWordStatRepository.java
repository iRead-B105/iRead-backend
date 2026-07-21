package com.iread.backend.report.repository;

import com.iread.backend.report.domain.StudentWordStatEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentWordStatRepository extends JpaRepository<StudentWordStatEntity, Long> {
    @EntityGraph(attributePaths = "word")
    List<StudentWordStatEntity> findAllByStudentId(Long studentId);
}
