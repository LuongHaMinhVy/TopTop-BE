package com.back.livestream.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "livestream_gifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LivestreamGift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livestream_id", nullable = false)
    private Livestream livestream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private GiftCatalog gift;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(nullable = false)
    @Builder.Default
    private int totalCoinPrice = 0;
}
