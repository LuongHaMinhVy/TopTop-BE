package com.back.livestream.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "gift_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GiftCatalog extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String iconUrl;

    @Column(length = 500)
    private String animationUrl;

    @Column(nullable = false)
    @Builder.Default
    private int coinPrice = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
