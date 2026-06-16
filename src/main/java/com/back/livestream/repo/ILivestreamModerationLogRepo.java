package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ILivestreamModerationLogRepo extends JpaRepository<LivestreamModerationLog, Long> {
    List<LivestreamModerationLog> findByLivestreamOrderByCreatedAtDesc(Livestream livestream);
}
