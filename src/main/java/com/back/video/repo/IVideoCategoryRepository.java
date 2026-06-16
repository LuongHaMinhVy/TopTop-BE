package com.back.video.repo;

import com.back.video.model.entity.VideoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVideoCategoryRepository extends JpaRepository<VideoCategory, Long> {
    List<VideoCategory> findByIsActiveTrue();
    Optional<VideoCategory> findByCodeIgnoreCase(String code);
    Optional<VideoCategory> findByCodeIgnoreCaseOrNameIgnoreCase(String code, String name);
}
