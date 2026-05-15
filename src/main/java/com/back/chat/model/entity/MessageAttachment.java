package com.back.chat.model.entity;

import com.back.common.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = "message_attachments")
public class MessageAttachment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false)
    private String type; // VIDEO, IMAGE, FILE, VIDEO_POST

    @Column
    private String url;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
