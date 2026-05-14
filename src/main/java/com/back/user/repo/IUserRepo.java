package com.back.user.repo;

import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepo extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String usernameOrEmail);
    
    java.util.List<User> findTop10ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String username, String nickname);
    java.util.List<User> findTop10ByOrderByCreatedAtDesc();
}
