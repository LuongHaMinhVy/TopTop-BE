package com.back.shop.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_title", nullable = false, length = 255)
    private String productTitle;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
