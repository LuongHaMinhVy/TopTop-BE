package com.back.shop.repo;

import com.back.shop.model.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByProductId(Long productId);
    void deleteAllByProductId(Long productId);
}
