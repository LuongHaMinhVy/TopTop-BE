package com.back.collection.mapper;

import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.collection.model.entity.CollectionVideo;
import com.back.collection.model.entity.VideoCollection;
import com.back.collection.repo.ICollectionVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectionMapper {

    private final ICollectionVideoRepository ICollectionVideoRepository;

    public CollectionResponseDTO toResponseDTO(VideoCollection collection) {
        Long videoCount = ICollectionVideoRepository.countByCollectionId(collection.getId());
        String coverUrl = ICollectionVideoRepository
                .findFirstAvailableByCollectionIdOrderByAddedAtDesc(collection.getId(), Limit.of(1))
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
