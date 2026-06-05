package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ShopCheckoutGroup {
    private Long shopId;
    private String shopName;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
}
