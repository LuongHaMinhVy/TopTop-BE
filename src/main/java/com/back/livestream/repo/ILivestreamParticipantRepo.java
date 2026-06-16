package com.back.livestream.repo;

import com.back.livestream.model.entity.Livestream;
import com.back.livestream.model.entity.LivestreamParticipant;
import com.back.livestream.model.enums.ParticipantRole;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ILivestreamParticipantRepo extends JpaRepository<LivestreamParticipant, Long> {
    Optional<LivestreamParticipant> findFirstByLivestreamAndUserOrderByIdDesc(Livestream livestream, User user);
    long countByLivestreamAndRole(Livestream livestream, ParticipantRole role);
}
