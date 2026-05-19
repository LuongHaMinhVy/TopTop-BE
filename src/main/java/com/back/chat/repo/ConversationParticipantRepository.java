package com.back.chat.repo;

import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.ConversationParticipant;
import com.back.chat.model.enums.ConversationStatus;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    @Query("SELECT cp.conversation FROM ConversationParticipant cp " +
           "WHERE cp.user = :user AND cp.conversation.deletedAt IS NULL " +
           "AND (cp.conversation.status = :activeStatus " +
           "OR (cp.conversation.status = :requestStatus AND cp.conversation.createdBy = :userId)) " +
           "ORDER BY cp.conversation.lastMessageAt DESC")
    Page<Conversation> findInboxConversationsByUser(
            @Param("user") User user,
            @Param("userId") Long userId,
            @Param("activeStatus") ConversationStatus activeStatus,
            @Param("requestStatus") ConversationStatus requestStatus,
            Pageable pageable
    );

    @Query("SELECT cp.conversation FROM ConversationParticipant cp " +
           "WHERE cp.user = :user AND cp.conversation.deletedAt IS NULL " +
           "AND cp.conversation.status = :requestStatus " +
           "AND cp.conversation.createdBy <> :userId " +
           "ORDER BY cp.conversation.lastMessageAt DESC")
    Page<Conversation> findRequestConversationsByUser(
            @Param("user") User user,
            @Param("userId") Long userId,
            @Param("requestStatus") ConversationStatus requestStatus,
            Pageable pageable
    );

    @Query("SELECT cp FROM ConversationParticipant cp " +
           "JOIN FETCH cp.user " +
           "WHERE cp.conversation = :conversation AND cp.user != :currentUser")
    Optional<ConversationParticipant> findTargetParticipant(@Param("conversation") Conversation conversation, @Param("currentUser") User currentUser);

    List<ConversationParticipant> findByConversation(Conversation conversation);
    
    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp " +
           "WHERE cp.user = :user " +
           "AND cp.conversation.deletedAt IS NULL " +
           "AND (cp.conversation.status = :activeStatus " +
           "OR (cp.conversation.status = :requestStatus AND cp.conversation.createdBy = :userId)) " +
           "AND cp.conversation.lastMessageAt IS NOT NULL " +
           "AND (cp.lastReadAt IS NULL OR cp.conversation.lastMessageAt > cp.lastReadAt)")
    long countUnreadInboxConversations(
            @Param("user") User user,
            @Param("userId") Long userId,
            @Param("activeStatus") ConversationStatus activeStatus,
            @Param("requestStatus") ConversationStatus requestStatus
    );

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp " +
           "WHERE cp.user = :user " +
           "AND cp.conversation.deletedAt IS NULL " +
           "AND cp.conversation.status = :requestStatus " +
           "AND cp.conversation.createdBy <> :userId " +
           "AND cp.conversation.lastMessageAt IS NOT NULL " +
           "AND (cp.lastReadAt IS NULL OR cp.conversation.lastMessageAt > cp.lastReadAt)")
    long countUnreadRequestConversations(
            @Param("user") User user,
            @Param("userId") Long userId,
            @Param("requestStatus") ConversationStatus requestStatus
    );
}
