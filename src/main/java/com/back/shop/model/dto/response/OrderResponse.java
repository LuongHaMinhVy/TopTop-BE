package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Long buyerId;
    private Long shopId;
    private String shopName;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String paymentStatus;
    private String shippingStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
