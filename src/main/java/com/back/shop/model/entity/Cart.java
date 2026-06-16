package com.back.shop.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "carts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
}
