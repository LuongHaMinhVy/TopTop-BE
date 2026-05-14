package com.back.collection.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.service.IVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.back.common.model.dto.response.Meta;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final IVideoService videoService;

    @GetMapping("/favorites/videos")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getFavoriteVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = videoService.getFavoriteVideos(pageable);
        
        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("collection.favorites.success", "Favorite videos retrieved successfully"))
                .data(videoPage.getContent())
                .meta(Meta.builder()
                        .page(videoPage.getNumber())
                        .size(videoPage.getSize())
                        .totalPages(videoPage.getTotalPages())
                        .totalElements(videoPage.getTotalElements())
                        .build())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
