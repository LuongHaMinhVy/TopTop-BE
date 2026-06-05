package com.back.shop.service;

import com.back.shop.model.dto.request.CreateProductRequest;
import com.back.shop.model.dto.request.UpdateProductRequest;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.model.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(Long id, UpdateProductRequest request);
    void deleteProduct(Long id);
    ProductResponse getProductById(Long id);
    ProductResponse getProductBySlug(Long shopId, String slug);
    Page<ProductResponse> getMyProducts(Pageable pageable);
    Page<ProductResponse> getProductsForAdmin(String status, String moderationStatus, Pageable pageable);
    Page<ProductResponse> getPublicProducts(String keyword, Long categoryId, Pageable pageable);
    Page<ProductResponse> getPublicProductsByShop(String shopSlug, Pageable pageable);
    ProductResponse moderateProduct(Long id, String moderationStatus);
    ProductReviewResponse createReview(com.back.shop.model.dto.request.CreateReviewRequest request);
    Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable);
}
