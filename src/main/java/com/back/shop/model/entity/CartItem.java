package com.back.shop.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CartItem extends BaseEntity {

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "selected", nullable = false)
    @Builder.Default
    private Boolean selected = true;
}
