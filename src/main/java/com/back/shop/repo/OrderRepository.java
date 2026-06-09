package com.back.shop.repo;

import com.back.shop.model.entity.Order;
import com.back.shop.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);
    Page<Order> findAllByShopId(Long shopId, Pageable pageable);
    long countByShopIdAndPaymentStatusIn(Long shopId, Collection<PaymentStatus> paymentStatuses);
    @Query(value = "select coalesce(sum(total_amount), 0) from orders where shop_id = :shopId and payment_status in (:paymentStatuses)", nativeQuery = true)
    BigDecimal sumPaidTotalByShopId(@Param("shopId") Long shopId, @Param("paymentStatuses") Collection<String> paymentStatuses);
    boolean existsByBuyerIdAndId(Long buyerId, Long id);
}
