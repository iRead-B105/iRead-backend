package com.iread.backend.student.repository;

import com.iread.backend.student.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    Optional<StudentEntity> findByIdAndTeacherId(Long id, Long teacherId);
}
