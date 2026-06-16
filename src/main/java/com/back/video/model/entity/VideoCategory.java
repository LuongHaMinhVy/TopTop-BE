package com.back.video.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "video_categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
