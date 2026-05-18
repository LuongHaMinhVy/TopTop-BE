package com.back.comment.controller;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import com.back.comment.service.ICommentService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.model.dto.response.Meta;
import com.back.common.utils.Translator;
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
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoCommentController {

    private final ICommentService commentService;

    @GetMapping("/{videoId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponseDTO> commentPage = commentService.getCommentsByVideo(videoId, pageable);

        return ResponseEntity.ok(ApiResponse.<List<CommentResponseDTO>>builder()
                .message(Translator.toLocale("comment.list.success", "Comments retrieved successfully"))
                .data(commentPage.getContent())
                .meta(Meta.from(commentPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{videoId}/comments")
    public ResponseEntity<ApiResponse<CommentResponseDTO>> addComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CommentRequestDTO requestDTO) {
        CommentResponseDTO data = commentService.addComment(videoId, requestDTO);
        return ResponseEntity.ok(ApiResponse.<CommentResponseDTO>builder()
                .message(Translator.toLocale("comment.add.success", "Comment added successfully"))
                .data(data)
                .status(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
