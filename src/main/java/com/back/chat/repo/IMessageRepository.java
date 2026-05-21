package com.back.chat.repo;

import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.Message;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IMessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);
    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation = :conversation " +
           "AND m.sender <> :currentUser " +
           "AND m.deletedAt IS NULL " +
           "AND (:lastReadAt IS NULL OR m.createdAt > :lastReadAt)")
    long countUnreadIncomingMessages(
            @Param("conversation") Conversation conversation,
            @Param("currentUser") User currentUser,
            @Param("lastReadAt") LocalDateTime lastReadAt
    );
}
