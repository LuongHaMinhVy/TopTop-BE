package com.back.shop.model.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SELLER_CONFIRMING,
    PACKING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED
}
