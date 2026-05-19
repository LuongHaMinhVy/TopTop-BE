package com.back.search.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "search_keyword_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_search_keyword_stat_normalized",
                        columnNames = {"normalized_keyword"}
                )
        },
        indexes = {
                @Index(name = "idx_search_keyword_count", columnList = "search_count"),
                @Index(name = "idx_search_keyword_last_time", columnList = "last_searched_at")
        }
)
public class SearchKeywordStat extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, length = 255)
    private String normalizedKeyword;

    @Column(name = "search_count", nullable = false)
    private Long searchCount;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;
}
