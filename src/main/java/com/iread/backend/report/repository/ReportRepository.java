package com.iread.backend.report.repository;

import com.iread.backend.report.domain.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    Optional<ReportEntity> findByIdAndStudentTeacherId(Long id, Long teacherId);
}
