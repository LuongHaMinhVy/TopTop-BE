package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.shop.model.dto.request.AddToCartRequest;
import com.back.shop.model.dto.request.UpdateCartItemRequest;
import com.back.shop.model.dto.response.CartResponse;
import com.back.shop.model.dto.response.CheckoutPreviewResponse;
import com.back.shop.service.ICartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        CartResponse data = cartService.getMyCart();
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Cart retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartResponse data = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Item added to cart")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse data = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Cart item updated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@PathVariable Long itemId) {
        CartResponse data = cartService.removeCartItem(itemId);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Item removed from cart")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/items/select")
    public ResponseEntity<ApiResponse<CartResponse>> selectCartItems(
            @RequestParam List<Long> itemIds,
            @RequestParam Boolean selected) {
        CartResponse data = cartService.selectCartItems(itemIds, selected);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Cart items updated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/checkout/preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> previewCheckout(@RequestParam List<Long> itemIds) {
        CheckoutPreviewResponse data = cartService.previewCheckout(itemIds);
        return ResponseEntity.ok(ApiResponse.<CheckoutPreviewResponse>builder()
                .message("Checkout preview generated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
