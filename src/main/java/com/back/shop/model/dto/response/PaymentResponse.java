package com.back.shop.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String provider;
    private String providerTransactionId;
    private String redirectUrl;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
}
