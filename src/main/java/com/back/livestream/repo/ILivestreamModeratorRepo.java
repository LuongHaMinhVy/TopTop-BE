package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamModerator;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ILivestreamModeratorRepo extends JpaRepository<LivestreamModerator, Long> {
    boolean existsByLivestreamAndUser(Livestream livestream, User user);
}
