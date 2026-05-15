package com.back.chat.repo;

import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);
    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);
}
