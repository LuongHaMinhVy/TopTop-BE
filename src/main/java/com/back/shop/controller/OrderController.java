package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.shop.model.dto.request.PlaceOrderRequest;
import com.back.shop.model.dto.response.OrderResponse;
import com.back.shop.service.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse data = orderService.placeOrder(request);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message(Translator.toLocale("order.place.success", "Order placed successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse data = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getMyOrders(pageable);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .message("Orders retrieved successfully")
                .data(orderPage.getContent())
                .meta(Meta.from(orderPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getShopOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getShopOrders(pageable);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .message("Shop orders retrieved successfully")
                .data(orderPage.getContent())
                .meta(Meta.from(orderPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        OrderResponse data = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order status updated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String provider = payload.getOrDefault("provider", "COD");
        String transactionId = payload.get("transactionId");
        OrderResponse data = orderService.payOrder(id, provider, transactionId);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message(Translator.toLocale("order.payment.success", "Order paid successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        OrderResponse data = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message(Translator.toLocale("order.cancel.success", "Order cancelled successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
