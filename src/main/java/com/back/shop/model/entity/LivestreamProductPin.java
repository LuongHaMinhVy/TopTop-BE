package com.back.shop.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "livestream_product_pins")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class LivestreamProductPin extends BaseEntity {

    @Column(name = "livestream_id", nullable = false)
    private Long livestreamId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "pinned_by", nullable = false)
    private Long pinnedBy;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
