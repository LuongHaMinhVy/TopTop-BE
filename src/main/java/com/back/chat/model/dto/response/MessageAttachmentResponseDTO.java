package com.back.chat.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachmentResponseDTO {
    private String type; // VIDEO, IMAGE, FILE, VIDEO_POST
    private Long videoId;
    private String videoUrl;
    private String url;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private String title;
    private String ownerUsername;
}
