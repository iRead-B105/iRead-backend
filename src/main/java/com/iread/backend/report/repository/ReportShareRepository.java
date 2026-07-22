package com.iread.backend.report.repository;

import com.iread.backend.report.domain.ReportShareEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportShareRepository extends JpaRepository<ReportShareEntity, Long> {

    @EntityGraph(attributePaths = {"report", "report.student"})
    Optional<ReportShareEntity> findByTokenHash(String tokenHash);

    List<ReportShareEntity> findAllByReportIdOrderByCreatedAtDesc(Long reportId);
}
