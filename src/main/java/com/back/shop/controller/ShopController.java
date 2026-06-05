package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.common.utils.redis.RateLimit;
import com.back.shop.model.dto.request.CreateShopRequest;
import com.back.shop.model.dto.request.UpdateShopRequest;
import com.back.shop.model.dto.response.ShopResponse;
import com.back.shop.service.IShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final IShopService shopService;

    @PostMapping
    @RateLimit(limit = 5, durationInSeconds = 600)
    public ResponseEntity<ApiResponse<ShopResponse>> createShop(@Valid @RequestBody CreateShopRequest request) {
        ShopResponse data = shopService.createShop(request);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message(Translator.toLocale("shop.create.success", "Shop registered successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(@Valid @RequestBody UpdateShopRequest request) {
        ShopResponse data = shopService.updateShop(request);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message(Translator.toLocale("shop.update.success", "Shop updated successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop() {
        ShopResponse data = shopService.getMyShop();
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopBySlug(@PathVariable String slug) {
        ShopResponse data = shopService.getShopBySlug(slug);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShopById(@PathVariable Long id) {
        ShopResponse data = shopService.getShopById(id);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
