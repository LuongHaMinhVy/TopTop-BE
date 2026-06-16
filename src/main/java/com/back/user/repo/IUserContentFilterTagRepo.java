package com.back.user.repo;

import com.back.user.model.entity.User;
import com.back.user.model.entity.UserContentFilterTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IUserContentFilterTagRepo extends JpaRepository<UserContentFilterTag, Long> {
    List<UserContentFilterTag> findByUserOrderByCreatedAtDesc(User user);
    Optional<UserContentFilterTag> findByUserAndTag(User user, String tag);
    void deleteByUserAndTag(User user, String tag);
}
