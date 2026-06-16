package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.shop.model.dto.request.CreateProductRequest;
import com.back.shop.model.dto.request.CreateReviewRequest;
import com.back.shop.model.dto.request.UpdateProductRequest;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.model.dto.response.ProductReviewResponse;
import com.back.shop.service.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse data = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message(Translator.toLocale("product.create.success", "Product created successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse data = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message(Translator.toLocale("product.update.success", "Product updated successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("product.delete.success", "Product deleted successfully"))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse data = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Product retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/shop/{shopId}/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable Long shopId, @PathVariable String slug) {
        ProductResponse data = productService.getProductBySlug(shopId, slug);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Product retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> productPage = productService.getMyProducts(pageable);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("My products retrieved successfully")
                .data(productPage.getContent())
                .meta(Meta.from(productPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPublicProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> productPage = productService.getPublicProducts(keyword, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("Products retrieved successfully")
                .data(productPage.getContent())
                .meta(Meta.from(productPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/public/shop/{shopSlug}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPublicProductsByShop(
            @PathVariable String shopSlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> productPage = productService.getPublicProductsByShop(shopSlug, pageable);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .message("Shop products retrieved")
                .data(productPage.getContent())
                .meta(Meta.from(productPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ProductReviewResponse data = productService.createReview(request);
        return ResponseEntity.ok(ApiResponse.<ProductReviewResponse>builder()
                .message(Translator.toLocale("review.create.success", "Review submitted successfully"))
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> getProductReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductReviewResponse> reviewPage = productService.getProductReviews(id, pageable);
        return ResponseEntity.ok(ApiResponse.<List<ProductReviewResponse>>builder()
                .message("Product reviews retrieved")
                .data(reviewPage.getContent())
                .meta(Meta.from(reviewPage))
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
