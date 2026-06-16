package com.back.sound.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "saved_sounds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_sounds_user_sound", columnNames = {"user_id", "sound_id"})
        },
        indexes = {
                @Index(name = "idx_saved_sounds_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_saved_sounds_sound_id", columnList = "sound_id")
        }
)
public class SavedSound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sound_id", nullable = false)
    private Sound sound;
}
