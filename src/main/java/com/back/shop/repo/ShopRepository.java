package com.back.shop.repo;

import com.back.shop.model.entity.Shop;
import com.back.shop.model.enums.ShopModerationStatus;
import com.back.shop.model.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByOwnerId(Long ownerId);
    Optional<Shop> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByOwnerId(Long ownerId);
    Page<Shop> findAllByStatusAndModerationStatus(ShopStatus status, ShopModerationStatus moderationStatus, Pageable pageable);
}
