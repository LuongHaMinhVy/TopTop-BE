package com.back.collection.service;

import com.back.collection.model.dto.request.CreateCollectionRequestDTO;
import com.back.collection.model.dto.request.UpdateCollectionRequestDTO;
import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.video.model.dto.request.VideoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICollectionService {

    Page<VideoResponseDTO> getFavoriteVideos(Pageable pageable);

    VideoResponseDTO saveVideo(Long videoId);

    VideoResponseDTO unsaveVideo(Long videoId);

    List<CollectionResponseDTO> getCollections();

    List<CollectionResponseDTO> getUserCollections(String username);

    CollectionResponseDTO getUserCollection(String username, Long collectionId);

    CollectionResponseDTO createCollection(CreateCollectionRequestDTO requestDTO);

    CollectionResponseDTO updateCollection(Long collectionId, UpdateCollectionRequestDTO requestDTO);

    void deleteCollection(Long collectionId);

    Page<VideoResponseDTO> getCollectionVideos(Long collectionId, Pageable pageable);

    Page<VideoResponseDTO> getUserCollectionVideos(String username, Long collectionId, Pageable pageable);

    VideoResponseDTO addVideoToCollection(Long collectionId, Long videoId);

    void removeVideoFromCollection(Long collectionId, Long videoId);
}
