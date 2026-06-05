package com.back.shop.repo;

import com.back.shop.model.entity.LivestreamProductPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivestreamProductPinRepository extends JpaRepository<LivestreamProductPin, Long> {
    List<LivestreamProductPin> findAllByLivestreamIdAndActiveOrderBySortOrderAsc(Long livestreamId, Boolean active);
    Optional<LivestreamProductPin> findByLivestreamIdAndProductId(Long livestreamId, Long productId);
    boolean existsByLivestreamIdAndProductId(Long livestreamId, Long productId);
}
