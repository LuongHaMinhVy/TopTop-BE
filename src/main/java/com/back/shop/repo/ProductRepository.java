package com.back.shop.repo;

import com.back.shop.model.entity.Product;
import com.back.shop.model.enums.ProductModerationStatus;
import com.back.shop.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByShopId(Long shopId, Pageable pageable);
    Page<Product> findAllByShopIdAndIsDeleted(Long shopId, Boolean isDeleted, Pageable pageable);
    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findAllByModerationStatus(ProductModerationStatus moderationStatus, Pageable pageable);
    Page<Product> findAllByStatusAndModerationStatus(ProductStatus status, ProductModerationStatus moderationStatus, Pageable pageable);
    Optional<Product> findByIdAndShopId(Long id, Long shopId);
    boolean existsByShopIdAndSlug(Long shopId, String slug);

    @Query("""
        SELECT p FROM Product p
        JOIN Shop s ON s.id = p.shopId
        WHERE p.status = :status
          AND p.moderationStatus = :modStatus
          AND p.isDeleted = false
          AND s.status = com.back.shop.model.enums.ShopStatus.ACTIVE
          AND s.moderationStatus = com.back.shop.model.enums.ShopModerationStatus.APPROVED
          AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
        """)
    Page<Product> findPublicProducts(@Param("status") ProductStatus status,
                                     @Param("modStatus") ProductModerationStatus modStatus,
                                     @Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        JOIN Shop s ON s.id = p.shopId
        WHERE s.slug = :shopSlug
          AND p.status = com.back.shop.model.enums.ProductStatus.ACTIVE
          AND p.moderationStatus = com.back.shop.model.enums.ProductModerationStatus.APPROVED
          AND p.isDeleted = false
          AND s.status = com.back.shop.model.enums.ShopStatus.ACTIVE
          AND s.moderationStatus = com.back.shop.model.enums.ShopModerationStatus.APPROVED
        """)
    Page<Product> findPublicProductsByShopSlug(@Param("shopSlug") String shopSlug, Pageable pageable);
}
