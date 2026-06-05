package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.shop.model.dto.response.ShopResponse;
import com.back.shop.service.IShopService;
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
@RequestMapping("/api/v1/admin/shops")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminShopModerationController {

    private final IShopService shopService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getShopsForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String moderationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ShopResponse> shopPage = shopService.getShopsForAdmin(status, moderationStatus, pageable);
        return ResponseEntity.ok(ApiResponse.<List<ShopResponse>>builder()
                .message("Shops retrieved successfully")
                .data(shopPage.getContent())
                .meta(Meta.from(shopPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}/moderate")
    public ResponseEntity<ApiResponse<ShopResponse>> moderateShop(
            @PathVariable Long id,
            @RequestParam String moderationStatus) {
        ShopResponse data = shopService.moderateShop(id, moderationStatus);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop moderation status updated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ShopResponse>> approveShop(@PathVariable Long id) {
        return moderateShop(id, "APPROVED");
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ShopResponse>> rejectShop(@PathVariable Long id) {
        return moderateShop(id, "REJECTED");
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<ShopResponse>> suspendShop(@PathVariable Long id) {
        ShopResponse data = shopService.suspendShop(id);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop suspended")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/unsuspend")
    public ResponseEntity<ApiResponse<ShopResponse>> unsuspendShop(@PathVariable Long id) {
        ShopResponse data = shopService.unsuspendShop(id);
        return ResponseEntity.ok(ApiResponse.<ShopResponse>builder()
                .message("Shop unsuspended")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
