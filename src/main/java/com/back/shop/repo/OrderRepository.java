package com.back.shop.repo;

import com.back.shop.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);
    Page<Order> findAllByShopId(Long shopId, Pageable pageable);
    boolean existsByBuyerIdAndId(Long buyerId, Long id);
}
