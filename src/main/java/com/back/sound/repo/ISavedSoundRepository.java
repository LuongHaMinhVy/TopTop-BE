package com.back.sound.repo;

import com.back.sound.model.entity.SavedSound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ISavedSoundRepository extends JpaRepository<SavedSound, Long> {

    boolean existsByUserIdAndSoundId(Long userId, Long soundId);

    Optional<SavedSound> findByUserIdAndSoundId(Long userId, Long soundId);

    @EntityGraph(attributePaths = {"sound", "sound.owner"})
    Page<SavedSound> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            select ss.sound.id
            from SavedSound ss
            where ss.user.id = :userId
              and ss.sound.id in :soundIds
            """)
    Set<Long> findSavedSoundIdsByUser(
            @Param("userId") Long userId,
            @Param("soundIds") Collection<Long> soundIds
    );
}
