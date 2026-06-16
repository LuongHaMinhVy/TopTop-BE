package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.shop.model.dto.response.OrderResponse;
import com.back.shop.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminOrderController {

    private final IOrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getOrdersForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .message("Orders retrieved successfully")
                .data(orderPage.getContent())
                .meta(Meta.from(orderPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderForAdmin(@PathVariable Long id) {
        OrderResponse data = orderService.getOrderForAdmin(id);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
