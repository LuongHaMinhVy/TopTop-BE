package com.back.hashtag.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.hashtag.model.dto.response.HashtagSuggestionResponseDTO;
import com.back.hashtag.service.IHashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final IHashtagService hashtagService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<HashtagSuggestionResponseDTO>>> getSuggestions(@RequestParam(required = false) String keyword) {
        List<HashtagSuggestionResponseDTO> data = hashtagService.getSuggestions(keyword);
        return ResponseEntity.ok(ApiResponse.<List<HashtagSuggestionResponseDTO>>builder()
                .message(Translator.toLocale("hashtag.suggestion.success", "Hashtag suggestions retrieved successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
