package com.back.report.repo;

import com.back.report.model.entity.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IReportReasonRepository extends JpaRepository<ReportReason, Long> {

    List<ReportReason> findByParentIsNullAndActiveTrueOrderBySortOrderAsc();

    List<ReportReason> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    Optional<ReportReason> findByIdAndActiveTrue(Long id);

    boolean existsByParentIdAndActiveTrue(Long parentId);
}
