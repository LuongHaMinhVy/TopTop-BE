package com.back.user.service;

import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.repo.IVideoRepository;
import com.back.video.service.VideoDeletionService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionCleanupService {
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final VideoDeletionService videoDeletionService;
    private final EntityManager entityManager;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void hardDeleteExpiredAccounts() {
        for (User user : userRepo.findByDeletionScheduledAtIsNotNullAndDeletionScheduledAtBefore(LocalDateTime.now())) {
            try {
                hardDeleteAccount(user);
                userRepo.flush();
                log.info("Hard deleted expired account: {}", user.getEmail());
            } catch (Exception ex) {
                log.warn("Could not hard delete expired account {}. It will be retried.", user.getEmail(), ex);
            }
        }
    }

    private void hardDeleteAccount(User user) {
        Long userId = user.getId();

        entityManager.createNativeQuery("""
                UPDATE sounds
                SET source_video_id = NULL
                WHERE source_video_id IN (SELECT id FROM videos WHERE user_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        videoRepository.findAllByUserId(userId)
                .forEach(videoDeletionService::hardDelete);

        deleteByUserId("video_not_interested", "user_id", userId);
        entityManager.createNativeQuery("""
                DELETE FROM message_attachments
                WHERE message_id IN (SELECT id FROM messages WHERE sender_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        deleteByUserId("comment_likes", "user_id", userId);
        entityManager.createNativeQuery("""
                DELETE FROM comment_likes
                WHERE comment_id IN (SELECT id FROM comments WHERE user_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE comments
                SET parent_id = NULL
                WHERE parent_id IN (SELECT id FROM comments WHERE user_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM comments WHERE user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();

        deleteByUserId("saved_sounds", "user_id", userId);
        entityManager.createNativeQuery("""
                DELETE FROM saved_sounds
                WHERE sound_id IN (SELECT id FROM sounds WHERE owner_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE videos
                SET sound_id = NULL
                WHERE sound_id IN (SELECT id FROM sounds WHERE owner_id = :userId)
                """)
                .setParameter("userId", userId)
                .executeUpdate();
        deleteByUserId("search_histories", "user_id", userId);
        deleteByUserId("user_content_filter_tags", "user_id", userId);
        deleteByUserId("video_collections", "user_id", userId);
        deleteByUserId("follows", "follower_id", userId);
        deleteByUserId("follows", "following_id", userId);
        deleteByUserId("user_blocks", "blocker_id", userId);
        deleteByUserId("user_blocks", "blocked_id", userId);
        deleteByUserId("notifications", "recipient_id", userId);
        deleteByUserId("notifications", "actor_id", userId);
        deleteByUserId("reports", "reporter_id", userId);
        deleteByUserId("conversation_participants", "user_id", userId);
        deleteByUserId("messages", "sender_id", userId);
        deleteByUserId("sounds", "owner_id", userId);
        deleteByUserId("verification_tokens", "user_id", userId);
        deleteByUserId("user_roles", "user_id", userId);

        userRepo.delete(user);
    }

    private void deleteByUserId(String table, String column, Long userId) {
        entityManager.createNativeQuery("DELETE FROM " + table + " WHERE " + column + " = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
