package com.back.search.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.search.model.enums.SearchSourceType;
import com.back.search.model.enums.SearchType;
import com.back.user.model.entity.User;
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
        name = "search_histories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_search_history_user_keyword_type",
                        columnNames = {"user_id", "normalized_keyword", "type"}
                )
        },
        indexes = {
                @Index(name = "idx_search_history_user_time", columnList = "user_id,searched_at"),
                @Index(name = "idx_search_history_keyword", columnList = "normalized_keyword")
        }
)
public class SearchHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, length = 255)
    private String normalizedKeyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SearchType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SearchSourceType sourceType;

    @Column(name = "result_target_id")
    private Long resultTargetId;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;
}
