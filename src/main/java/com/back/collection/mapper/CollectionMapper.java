package com.back.collection.mapper;

import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.collection.model.entity.CollectionVideo;
import com.back.collection.model.entity.VideoCollection;
import com.back.collection.repo.CollectionVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectionMapper {

    private final CollectionVideoRepository collectionVideoRepository;

    public CollectionResponseDTO toResponseDTO(VideoCollection collection) {
        Long videoCount = collectionVideoRepository.countByCollectionId(collection.getId());
        String coverUrl = collectionVideoRepository
                .findFirstByCollectionIdOrderByAddedAtDesc(collection.getId())
                .map(CollectionVideo::getVideo)
                .map(video -> video.getThumbnailUrl() != null ? video.getThumbnailUrl() : video.getFileUrl())
                .orElse(null);

        return CollectionResponseDTO.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .videoCount(videoCount)
                .coverUrl(coverUrl)
                .isPublic(collection.getIsPublic())
                .ownerUsername(collection.getUser().getUsername())
                .createdAt(collection.getCreatedAt())
                .build();
    }
}
