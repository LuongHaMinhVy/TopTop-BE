package com.back.livestream.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "livestream_bans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"livestream_id","user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LivestreamBan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livestream_id", nullable = false)
    private Livestream livestream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by", nullable = false)
    private User bannedBy;
}
