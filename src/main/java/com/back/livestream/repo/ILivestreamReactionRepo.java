package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamReaction;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ILivestreamReactionRepo extends JpaRepository<LivestreamReaction, Long> {
    Optional<LivestreamReaction> findByLivestreamAndUserAndType(Livestream livestream, User user, String type);

    @Query("SELECT COALESCE(SUM(r.count), 0) FROM LivestreamReaction r WHERE r.livestream = :livestream AND r.type = :type")
    long sumCountByLivestreamAndType(@Param("livestream") Livestream livestream, @Param("type") String type);
}
