package com.back.collection.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollectionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long videoCount;
    private String coverUrl;
    private Boolean isPublic;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
