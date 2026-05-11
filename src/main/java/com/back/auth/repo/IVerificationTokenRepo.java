package com.back.auth.repo;

import com.back.auth.model.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IVerificationTokenRepo extends JpaRepository<VerificationToken, Long>{
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser_Email(String email);
}