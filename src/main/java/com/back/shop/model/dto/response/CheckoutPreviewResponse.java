package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CheckoutPreviewResponse {
    private List<ShopCheckoutGroup> shops;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
}
