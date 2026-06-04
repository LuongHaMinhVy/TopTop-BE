package com.back.livestream.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.livestream.model.dto.response.AdminLivestreamResponseDTO;
import com.back.livestream.model.enums.LivestreamStatus;
import com.back.livestream.service.IAdminLivestreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/livestreams")
@RequiredArgsConstructor
public class AdminLivestreamController {

    private final IAdminLivestreamService adminLivestreamService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminLivestreamResponseDTO>>> listLivestreams(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LivestreamStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminLivestreamResponseDTO> data =
                adminLivestreamService.listLivestreams(keyword, status, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<AdminLivestreamResponseDTO>>builder()
                .message(Translator.toLocale("admin.livestream.list.success", "Livestreams loaded successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{livestreamId}/end")
    public ResponseEntity<ApiResponse<AdminLivestreamResponseDTO>> endLivestream(
            @PathVariable Long livestreamId) {
        AdminLivestreamResponseDTO data = adminLivestreamService.endLivestream(livestreamId);

        return ResponseEntity.ok(ApiResponse.<AdminLivestreamResponseDTO>builder()
                .message(Translator.toLocale("admin.livestream.end.success", "Livestream ended successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
