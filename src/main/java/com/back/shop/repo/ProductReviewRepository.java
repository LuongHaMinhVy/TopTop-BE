package com.back.shop.repo;

import com.back.shop.model.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Page<ProductReview> findAllByProductId(Long productId, Pageable pageable);
    boolean existsByOrderItemId(Long orderItemId);
}
