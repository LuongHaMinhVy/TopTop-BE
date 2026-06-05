package com.back.shop.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateShopRequest {
    @Size(max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    private String avatarUrl;
    private String bannerUrl;
}
