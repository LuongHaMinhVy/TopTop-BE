package com.back.shop.service;

import com.back.shop.model.dto.request.PlaceOrderRequest;
import com.back.shop.model.dto.response.OrderResponse;
import com.back.shop.model.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IOrderService {
    OrderResponse placeOrder(PlaceOrderRequest request);
    OrderResponse getOrderById(Long id);
    Page<OrderResponse> getMyOrders(Pageable pageable);
    Page<OrderResponse> getShopOrders(Pageable pageable);
    Page<OrderResponse> getOrdersForAdmin(Pageable pageable);
    OrderResponse getOrderForAdmin(Long id);
    OrderResponse updateOrderStatus(Long orderId, String status);
    PaymentResponse payOrder(Long orderId, String paymentProvider);
    OrderResponse completePayment(Long orderId, String paymentProvider, String providerReference);
    OrderResponse cancelOrder(Long orderId);
}
