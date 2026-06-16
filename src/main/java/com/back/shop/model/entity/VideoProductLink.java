package com.back.shop.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_product_links")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoProductLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
