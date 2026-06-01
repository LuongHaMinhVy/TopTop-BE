package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamGift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ILivestreamGiftRepo extends JpaRepository<LivestreamGift, Long> {
    List<LivestreamGift> findByLivestreamOrderByCreatedAtDesc(Livestream livestream);
}
