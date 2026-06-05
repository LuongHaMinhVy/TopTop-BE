package com.back.shop.repo;

import com.back.shop.model.entity.VideoProductLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoProductLinkRepository extends JpaRepository<VideoProductLink, Long> {
    List<VideoProductLink> findAllByVideoIdOrderBySortOrderAsc(Long videoId);
    Optional<VideoProductLink> findByVideoIdAndProductId(Long videoId, Long productId);
    boolean existsByVideoIdAndProductId(Long videoId, Long productId);
    void deleteByVideoIdAndProductId(Long videoId, Long productId);
}
