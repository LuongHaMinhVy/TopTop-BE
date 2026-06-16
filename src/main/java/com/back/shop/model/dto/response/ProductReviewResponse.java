package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductReviewResponse {
    private Long id;
    private Long productId;
    private Long orderItemId;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
