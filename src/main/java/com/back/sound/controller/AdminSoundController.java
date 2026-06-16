package com.back.sound.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
import com.back.sound.model.dto.request.CreateSoundRequestDTO;
import com.back.sound.model.dto.request.UpdateSoundRequestDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.service.ISoundService;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sounds")
public class AdminSoundController {

    private final ISoundService soundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SoundResponseDTO>>> listSounds(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SoundResponseDTO> soundPage = soundService.searchAdminSounds(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.<List<SoundResponseDTO>>builder()
                .message(Translator.toLocale("sound.list.success", "Sounds loaded successfully"))
                .data(soundPage.getContent())
                .meta(Meta.from(soundPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SoundResponseDTO>> createSound(@RequestBody @Valid CreateSoundRequestDTO requestDTO) {
        SoundResponseDTO data = soundService.createSound(requestDTO);
        return ResponseEntity.ok(ApiResponse.<SoundResponseDTO>builder()
                .message(Translator.toLocale("sound.create.success", "Sound created successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{soundId}")
    public ResponseEntity<ApiResponse<SoundResponseDTO>> updateSound(
            @PathVariable Long soundId,
            @RequestBody @Valid UpdateSoundRequestDTO requestDTO) {
        SoundResponseDTO data = soundService.updateSound(soundId, requestDTO);
        return ResponseEntity.ok(ApiResponse.<SoundResponseDTO>builder()
                .message(Translator.toLocale("sound.update.success", "Sound updated successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{soundId}")
    public ResponseEntity<ApiResponse<Void>> deleteSound(@PathVariable Long soundId) {
        soundService.deleteSound(soundId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("sound.delete.success", "Sound deleted successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
