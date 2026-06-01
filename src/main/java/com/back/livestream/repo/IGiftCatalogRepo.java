package com.back.livestream.repo;

import com.back.livestream.model.entity.GiftCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IGiftCatalogRepo extends JpaRepository<GiftCatalog, Long> {
    List<GiftCatalog> findByIsActiveTrue();
}
