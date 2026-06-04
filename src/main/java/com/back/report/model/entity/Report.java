package com.back.report.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.report.model.enums.ReportResolutionAction;
import com.back.report.model.enums.ReportStatus;
import com.back.report.model.enums.ReportTargetType;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Report extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", nullable = false)
    private ReportReason reason;

    @Column(nullable = false, length = 100)
    private String reasonCode;

    @Column(length = 1000)
    private String additionalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private Long reviewedBy;

    @Column(length = 1000)
    private String reviewNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ReportResolutionAction resolutionAction;

    private LocalDateTime reviewedAt;
}
