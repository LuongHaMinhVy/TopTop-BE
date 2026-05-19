package com.back.search.repo;

import com.back.search.model.entity.SearchKeywordStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchKeywordStatRepository extends JpaRepository<SearchKeywordStat, Long> {
    Optional<SearchKeywordStat> findByNormalizedKeyword(String normalizedKeyword);

    List<SearchKeywordStat> findByNormalizedKeywordContainingIgnoreCaseOrderBySearchCountDescLastSearchedAtDesc(
            String normalizedKeyword,
            Pageable pageable
    );

    List<SearchKeywordStat> findByOrderBySearchCountDescLastSearchedAtDesc(Pageable pageable);
}
