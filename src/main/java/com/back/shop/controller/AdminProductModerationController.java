package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.service.IProductService;
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
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminProductModerationController {

    private final IProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String moderationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> productPage = productService.getProductsForAdmin(status, moderationStatus, pageable);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("Products retrieved successfully")
                .data(productPage.getContent())
                .meta(Meta.from(productPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}/moderate")
    public ResponseEntity<ApiResponse<ProductResponse>> moderateProduct(
            @PathVariable Long id,
            @RequestParam String moderationStatus) {
        ProductResponse data = productService.moderateProduct(id, moderationStatus);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Product moderation status updated")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(@PathVariable Long id) {
        return moderateProduct(id, "APPROVED");
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponse>> rejectProduct(@PathVariable Long id) {
        return moderateProduct(id, "REJECTED");
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<ProductResponse>> banProduct(@PathVariable Long id) {
        return moderateProduct(id, "REJECTED");
    }
}
