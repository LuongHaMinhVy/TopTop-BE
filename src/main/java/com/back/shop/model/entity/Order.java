package com.back.shop.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.shop.model.enums.OrderStatus;
import com.back.shop.model.enums.PaymentStatus;
import com.back.shop.model.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Order extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 30)
    @Builder.Default
    private ShippingStatus shippingStatus = ShippingStatus.NOT_SHIPPED;

    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false, columnDefinition = "TEXT")
    private String receiverAddress;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
