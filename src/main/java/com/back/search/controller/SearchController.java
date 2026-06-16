package com.back.search.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.search.model.dto.request.SaveSearchHistoryRequestDTO;
import com.back.search.model.dto.response.*;
import com.back.search.service.ISearchHistoryService;
import com.back.search.service.ISearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final ISearchService ISearchService;
    private final ISearchHistoryService ISearchHistoryService;

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<SearchTopResponseDTO>> top(
            @RequestParam("q") String keyword,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<SearchTopResponseDTO>builder()
                .message("Tìm kiếm thành công")
                .data(ISearchService.searchTop(keyword, authentication))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<SearchUserResponseDTO>>> users(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Page<SearchUserResponseDTO> page = ISearchService.searchUsers(keyword, pageable, authentication);
        return paged("Tìm kiếm người dùng thành công", page);
    }

    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<List<SearchVideoResponseDTO>>> videos(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Page<SearchVideoResponseDTO> page = ISearchService.searchVideos(keyword, pageable, authentication);
        return paged("Tìm kiếm video thành công", page);
    }

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<SearchLiveResponseDTO>>> live(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Page<SearchLiveResponseDTO> page = ISearchService.searchLive(keyword, pageable, authentication);
        return paged("Tìm kiếm LIVE thành công", page);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionResponseDTO>> suggestions(
            @RequestParam(value = "q", defaultValue = "") String keyword,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<SearchSuggestionResponseDTO>builder()
                .message("Gợi ý tìm kiếm thành công")
                .data(ISearchService.suggestions(keyword, authentication))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SearchHistoryResponseDTO>>> history(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<SearchHistoryResponseDTO>>builder()
                .message("Lịch sử tìm kiếm")
                .data(ISearchHistoryService.getMyHistory(authentication))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/history")
    public ResponseEntity<ApiResponse<SearchHistoryResponseDTO>> saveHistory(
            Authentication authentication,
            @Valid @RequestBody SaveSearchHistoryRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.<SearchHistoryResponseDTO>builder()
                .message("Đã lưu lịch sử tìm kiếm")
                .data(ISearchHistoryService.save(authentication, request))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            Authentication authentication,
            @PathVariable Long historyId
    ) {
        ISearchHistoryService.deleteOne(authentication, historyId);
        return ResponseEntity.ok(empty("Đã xóa lịch sử tìm kiếm"));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(Authentication authentication) {
        ISearchHistoryService.clear(authentication);
        return ResponseEntity.ok(empty("Đã xóa lịch sử tìm kiếm"));
    }

    private <T> ResponseEntity<ApiResponse<List<T>>> paged(String message, Page<T> page) {
        return ResponseEntity.ok(ApiResponse.<List<T>>builder()
                .message(message)
                .data(page.getContent())
                .meta(Meta.from(page))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ApiResponse<Void> empty(String message) {
        return ApiResponse.<Void>builder()
                .message(message)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
