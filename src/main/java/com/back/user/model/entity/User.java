package com.back.user.model.entity;

import com.back.common.model.entity.BaseEntity;
import com.back.user.model.enums.AccountType;
import com.back.user.model.enums.Gender;
import com.back.user.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
})
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 24)
    private String username;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 80)
    private String bio;

    @Column
    private String avatarUrl;

    @Column
    private String coverUrl;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private LocalDate dateOfBirth;

    @Builder.Default
    @Column(nullable = false)
    private Long followersCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long followingCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long totalLikes = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long videoCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPrivate = false;

    @Builder.Default
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 50)
    private String instagramHandle;

    @Column(length = 50)
    private String youtubeHandle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 100)
    private String region;

    @Column(length = 10)
    private String languagePreference;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowComments = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowDuet = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowStitch = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowDownload = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowMessageFromEveryone = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

}