package com.back.chat.repo;

import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.ConversationParticipant;
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
           "ORDER BY cp.conversation.lastMessageAt DESC")
    Page<Conversation> findConversationsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT cp FROM ConversationParticipant cp " +
           "JOIN FETCH cp.user " +
           "WHERE cp.conversation = :conversation AND cp.user != :currentUser")
    Optional<ConversationParticipant> findTargetParticipant(@Param("conversation") Conversation conversation, @Param("currentUser") User currentUser);

    List<ConversationParticipant> findByConversation(Conversation conversation);
    
    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp " +
           "WHERE cp.user = :user AND cp.conversation.lastMessageAt > cp.lastReadAt")
    long countUnreadConversations(@Param("user") User user);
}
