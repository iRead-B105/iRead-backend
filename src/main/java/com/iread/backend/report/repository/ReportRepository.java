package com.iread.backend.report.repository;

import com.iread.backend.report.domain.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    Optional<ReportEntity> findByIdAndStudentTeacherId(Long id, Long teacherId);
    List<ReportEntity> findAllByStudentTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<ReportEntity> findAllByStudentIdAndStudentTeacherIdOrderByCreatedAtDesc(
            Long studentId,
            Long teacherId
    );
    Optional<ReportEntity> findByStudentIdAndStartDateAndEndDate(
            Long studentId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
