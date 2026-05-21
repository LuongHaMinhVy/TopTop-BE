package com.back.collection.repo;

import com.back.collection.model.entity.VideoCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVideoCollectionRepository extends JpaRepository<VideoCollection, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<VideoCollection> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<VideoCollection> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<VideoCollection> findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<VideoCollection> findByIdAndUserUsernameIgnoreCase(Long id, String username);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
