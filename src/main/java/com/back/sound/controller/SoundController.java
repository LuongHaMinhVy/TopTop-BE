package com.back.sound.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.sound.model.dto.response.SoundDetailResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.dto.response.SoundStatsResponseDTO;
import com.back.sound.model.enums.SoundType;
import com.back.sound.service.ISoundService;
import com.back.video.model.dto.request.VideoResponseDTO;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/sounds")
public class SoundController {

    private final ISoundService soundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SoundResponseDTO>>> listSounds(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SoundType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SoundResponseDTO> soundPage = soundService.searchPublicSounds(keyword, type, pageable);

        return ResponseEntity.ok(ApiResponse.<List<SoundResponseDTO>>builder()
                .message(Translator.toLocale("sound.list.success", "Sounds loaded successfully"))
                .data(soundPage.getContent())
                .meta(Meta.from(soundPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{soundId}")
    public ResponseEntity<ApiResponse<SoundDetailResponseDTO>> getSound(@PathVariable Long soundId) {
        SoundDetailResponseDTO data = soundService.getSoundDetail(soundId);
        return ResponseEntity.ok(ApiResponse.<SoundDetailResponseDTO>builder()
                .message(Translator.toLocale("sound.detail.success", "Sound loaded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<SoundResponseDTO>>> getFavoriteSounds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SoundResponseDTO> soundPage = soundService.getFavoriteSounds(pageable);

        return ResponseEntity.ok(ApiResponse.<List<SoundResponseDTO>>builder()
                .message(Translator.toLocale("sound.favorites.success", "Favorite sounds loaded successfully"))
                .data(soundPage.getContent())
                .meta(Meta.from(soundPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{soundId}/save")
    public ResponseEntity<ApiResponse<SoundStatsResponseDTO>> saveSound(@PathVariable Long soundId) {
        SoundStatsResponseDTO data = soundService.saveSound(soundId);
        return ResponseEntity.ok(ApiResponse.<SoundStatsResponseDTO>builder()
                .message(Translator.toLocale("sound.save.success", "Sound saved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{soundId}/save")
    public ResponseEntity<ApiResponse<SoundStatsResponseDTO>> unsaveSound(@PathVariable Long soundId) {
        SoundStatsResponseDTO data = soundService.unsaveSound(soundId);
        return ResponseEntity.ok(ApiResponse.<SoundStatsResponseDTO>builder()
                .message(Translator.toLocale("sound.unsave.success", "Sound removed from favorites"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{soundId}/videos")
    public ResponseEntity<ApiResponse<List<VideoResponseDTO>>> getSoundVideos(
            @PathVariable Long soundId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponseDTO> videoPage = soundService.getSoundVideos(soundId, pageable);

        return ResponseEntity.ok(ApiResponse.<List<VideoResponseDTO>>builder()
                .message(Translator.toLocale("sound.videos.success", "Sound videos loaded successfully"))
                .data(videoPage.getContent())
                .meta(Meta.from(videoPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
