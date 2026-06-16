package com.back.hashtag.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "hashtags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Hashtag extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "post_count")
    @Builder.Default
    private Long postCount = 0L;
}
