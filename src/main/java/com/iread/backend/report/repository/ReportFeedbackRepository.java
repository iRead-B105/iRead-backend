package com.iread.backend.report.repository;

import com.iread.backend.report.domain.ReportFeedbackEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportFeedbackRepository extends JpaRepository<ReportFeedbackEntity, Long> {

    @EntityGraph(attributePaths = {"reportShare", "reportShare.report", "reportShare.report.student"})
    List<ReportFeedbackEntity> findAllByReportShareReportStudentTeacherIdOrderByCreatedAtDesc(Long teacherId);

    @EntityGraph(attributePaths = {"reportShare", "reportShare.report", "reportShare.report.student"})
    List<ReportFeedbackEntity> findAllByReportShareReportStudentTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(
            Long teacherId
    );

    @EntityGraph(attributePaths = {"reportShare", "reportShare.report", "reportShare.report.student"})
    Optional<ReportFeedbackEntity> findByIdAndReportShareReportStudentTeacherId(Long id, Long teacherId);
}
