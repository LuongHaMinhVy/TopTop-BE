package com.back.collection.controller;

import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.collection.service.ICollectionService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.video.model.dto.request.VideoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{username}/collections")
@RequiredArgsConstructor
public class UserCollectionController {

    private final ICollectionService collectionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionResponseDTO>>> getUserCollections(
            @PathVariable String username) {
        List<CollectionResponseDTO> data = collectionService.getUserCollections(username);

        return ResponseEntity.ok(ApiResponse.<List<CollectionResponseDTO>>builder()
                .message(Translator.toLocale("collection.user_list.success", "User collections retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponseDTO>> getUserCollection(
            @PathVariable String username,
            @PathVariable Long collectionId) {
        CollectionResponseDTO data = collectionService.getUserCollection(username, collectionId);

        return ResponseEntity.ok(ApiResponse.<CollectionResponseDTO>builder()
                .message(Translator.toLocale("collection.detail.success", "Collection retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{collectionId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getUserCollectionVideos(
            @PathVariable String username,
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = collectionService.getUserCollectionVideos(username, collectionId, pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("collection.videos.success", "Collection videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
