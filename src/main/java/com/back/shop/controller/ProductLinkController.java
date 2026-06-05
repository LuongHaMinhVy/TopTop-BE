package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.shop.model.dto.request.LinkVideoProductRequest;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.service.IProductLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shop-links")
@RequiredArgsConstructor
public class ProductLinkController {

    private final IProductLinkService productLinkService;

    @PostMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<Void>> linkProductsToVideo(
            @PathVariable Long videoId,
            @Valid @RequestBody LinkVideoProductRequest request) {
        productLinkService.linkProductsToVideo(videoId, request.getProductIds());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Products linked to video successfully")
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByVideo(@PathVariable Long videoId) {
        List<ProductResponse> data = productLinkService.getProductsByVideo(videoId);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("Products retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/livestream/{livestreamId}/pin/{productId}")
    public ResponseEntity<ApiResponse<Void>> pinProductToLivestream(
            @PathVariable Long livestreamId,
            @PathVariable Long productId) {
        productLinkService.pinProductToLivestream(livestreamId, productId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Product pinned successfully")
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/livestream/{livestreamId}/pin/{productId}")
    public ResponseEntity<ApiResponse<Void>> unpinProductFromLivestream(
            @PathVariable Long livestreamId,
            @PathVariable Long productId) {
        productLinkService.unpinProductFromLivestream(livestreamId, productId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Product unpinned successfully")
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/livestream/{livestreamId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByLivestream(@PathVariable Long livestreamId) {
        List<ProductResponse> data = productLinkService.getProductsByLivestream(livestreamId);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("Products retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
