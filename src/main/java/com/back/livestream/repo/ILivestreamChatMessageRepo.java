package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ILivestreamChatMessageRepo extends JpaRepository<LivestreamChatMessage, Long> {

    @Query("""
            SELECT m FROM LivestreamChatMessage m
            JOIN FETCH m.sender
            WHERE m.livestream = :livestream
              AND m.isHidden = false
            ORDER BY m.createdAt DESC
            """)
    Page<LivestreamChatMessage> findVisibleMessages(@Param("livestream") Livestream livestream, Pageable pageable);

    @Query("""
            SELECT m FROM LivestreamChatMessage m
            JOIN FETCH m.sender
            WHERE m.livestream = :livestream AND m.isPinned = true AND m.isHidden = false
            """)
    List<LivestreamChatMessage> findPinnedMessages(@Param("livestream") Livestream livestream);
}
