package com.back.hashtag.repo;

import com.back.hashtag.model.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface IHashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String name);
    
    List<Hashtag> findTop10ByNameContainingIgnoreCaseOrderByPostCountDesc(String name);
    List<Hashtag> findTop10ByOrderByPostCountDesc();
}
