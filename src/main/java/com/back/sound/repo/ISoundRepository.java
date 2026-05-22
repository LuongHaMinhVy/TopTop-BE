package com.back.sound.repo;

import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISoundRepository extends JpaRepository<Sound, Long> {

    Optional<Sound> findByIdAndIsDeletedFalse(Long id);

    @Query("""
            SELECT s FROM Sound s
            WHERE s.isDeleted = false
              AND s.isActive = true
              AND s.isPublic = true
              AND (:type IS NULL OR s.type = :type)
              AND (
                :keyword IS NULL
                OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(s.artistName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY s.usageCount DESC, s.createdAt DESC
            """)
    Page<Sound> searchPublicSounds(
            @Param("keyword") String keyword,
            @Param("type") SoundType type,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM Sound s
            WHERE s.isDeleted = false
              AND (
                :keyword IS NULL
                OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(s.artistName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY s.createdAt DESC
            """)
    Page<Sound> searchAdminSounds(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Sound s SET s.usageCount = s.usageCount + 1 WHERE s.id = :soundId")
    void incrementUsageCount(@Param("soundId") Long soundId);

    @Modifying
    @Query("UPDATE Sound s SET s.usageCount = CASE WHEN s.usageCount > 0 THEN s.usageCount - 1 ELSE 0 END WHERE s.id = :soundId")
    void decrementUsageCount(@Param("soundId") Long soundId);

    @Modifying
    @Query("UPDATE Sound s SET s.savedCount = s.savedCount + 1 WHERE s.id = :soundId")
    void incrementSavedCount(@Param("soundId") Long soundId);

    @Modifying
    @Query("UPDATE Sound s SET s.savedCount = CASE WHEN s.savedCount > 0 THEN s.savedCount - 1 ELSE 0 END WHERE s.id = :soundId")
    void decrementSavedCount(@Param("soundId") Long soundId);
}
