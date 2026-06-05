package com.back.shop.repo;

import com.back.shop.model.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findAllByProductIdOrderBySortOrderAsc(Long productId);
    void deleteAllByProductId(Long productId);
}
