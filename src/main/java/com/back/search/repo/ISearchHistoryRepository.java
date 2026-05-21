package com.back.search.repo;

import com.back.search.model.entity.SearchHistory;
import com.back.search.model.enums.SearchType;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserOrderBySearchedAtDesc(User user, Pageable pageable);

    List<SearchHistory> findByUserAndNormalizedKeywordContainingIgnoreCaseOrderBySearchedAtDesc(
            User user,
            String normalizedKeyword,
            Pageable pageable
    );

    Optional<SearchHistory> findByUserAndNormalizedKeywordAndType(
            User user,
            String normalizedKeyword,
            SearchType type
    );

    void deleteByUser(User user);
}
