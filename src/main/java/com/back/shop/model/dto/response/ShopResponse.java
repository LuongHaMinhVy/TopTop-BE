package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopResponse {
    private Long id;
    private Long ownerId;
    private String name;
    private String slug;
    private String description;
    private String avatarUrl;
    private String bannerUrl;
    private String status;
    private String moderationStatus;
}
