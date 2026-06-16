package com.back.collection.controller;

import com.back.collection.model.dto.request.CreateCollectionRequestDTO;
import com.back.collection.model.dto.request.UpdateCollectionRequestDTO;
import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.collection.service.ICollectionService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.video.model.dto.request.VideoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final ICollectionService collectionService;

    @GetMapping("/favorites/videos")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getFavoriteVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = collectionService.getFavoriteVideos(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("collection.favorites.success", "Favorite videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/favorites/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> saveVideo(@PathVariable Long videoId) {
        VideoResponseDTO data = collectionService.saveVideo(videoId);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("collection.save.success", "Video saved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/favorites/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> unsaveVideo(@PathVariable Long videoId) {
        VideoResponseDTO data = collectionService.unsaveVideo(videoId);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("collection.unsave.success", "Video removed from favorites"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionResponseDTO>>> getCollections() {
        List<CollectionResponseDTO> data = collectionService.getCollections();
        return ResponseEntity.ok(ApiResponse.<List<CollectionResponseDTO>>builder()
                .message(Translator.toLocale("collection.list.success", "Collections retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionResponseDTO>> createCollection(
            @Valid @RequestBody CreateCollectionRequestDTO requestDTO) {
        CollectionResponseDTO data = collectionService.createCollection(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CollectionResponseDTO>builder()
                .message(Translator.toLocale("collection.create.success", "Collection created successfully"))
                .data(data)
                .status(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponseDTO>> updateCollection(
            @PathVariable Long collectionId,
            @Valid @RequestBody UpdateCollectionRequestDTO requestDTO) {
        CollectionResponseDTO data = collectionService.updateCollection(collectionId, requestDTO);
        return ResponseEntity.ok(ApiResponse.<CollectionResponseDTO>builder()
                .message(Translator.toLocale("collection.update.success", "Collection updated successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteCollection(@PathVariable Long collectionId) {
        collectionService.deleteCollection(collectionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("collection.delete.success", "Collection deleted successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{collectionId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getCollectionVideos(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = collectionService.getCollectionVideos(collectionId, pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("collection.videos.success", "Collection videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{collectionId}/videos/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponseDTO>> addVideoToCollection(
            @PathVariable Long collectionId,
            @PathVariable Long videoId) {
        VideoResponseDTO data = collectionService.addVideoToCollection(collectionId, videoId);
        return ResponseEntity.ok(ApiResponse.<VideoResponseDTO>builder()
                .message(Translator.toLocale("collection.video_add.success", "Video added to collection"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{collectionId}/videos/{videoId}")
    public ResponseEntity<ApiResponse<Void>> removeVideoFromCollection(
            @PathVariable Long collectionId,
            @PathVariable Long videoId) {
        collectionService.removeVideoFromCollection(collectionId, videoId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("collection.video_remove.success", "Video removed from collection"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
