package com.back.report.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.report.model.enums.ReportReasonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "report_reasons")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReason extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ReportReason parent;

    @Column(nullable = false)
    private String labelEn;

    @Column(nullable = false)
    private String labelVi;

    private String descriptionEn;
    private String descriptionVi;

    private String policyTextEn;
    private String policyTextVi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReasonType type;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active;
}
