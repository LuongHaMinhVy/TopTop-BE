package com.back.report.repo;

import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import com.back.report.model.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndReasonId(
        Long reporterId,
        ReportTargetType targetType,
        Long targetId,
        Long reasonId
    );

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}
