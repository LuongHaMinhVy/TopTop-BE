package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamBan;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ILivestreamBanRepo extends JpaRepository<LivestreamBan, Long> {
    boolean existsByLivestreamAndUser(Livestream livestream, User user);
    Optional<LivestreamBan> findByLivestreamAndUser(Livestream livestream, User user);
}
