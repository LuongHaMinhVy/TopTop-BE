package com.back.comment.controller;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentLikeResponseDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import com.back.comment.service.ICommentService;
import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.common.model.dto.response.Meta;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final ICommentService commentService;

    @PostMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<CommentResponseDTO>> addComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CommentRequestDTO requestDTO) {
        CommentResponseDTO data = commentService.addComment(videoId, requestDTO);
        return ResponseEntity.ok(ApiResponse.<CommentResponseDTO>builder()
                .message(Translator.toLocale("comment.add.success", "Comment added successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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

    @GetMapping("/{id}/replies")
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getReplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponseDTO> replyPage = commentService.getReplies(id, pageable);

        return ResponseEntity.ok(ApiResponse.<List<CommentResponseDTO>>builder()
                .message(Translator.toLocale("comment.replies.success", "Replies retrieved successfully"))
                .data(replyPage.getContent())
                .meta(Meta.from(replyPage))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<ApiResponse<CommentResponseDTO>> addReply(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDTO requestDTO) {
        CommentResponseDTO data = commentService.addReply(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.<CommentResponseDTO>builder()
                .message(Translator.toLocale("comment.reply.success", "Reply added successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(Translator.toLocale("comment.delete.success", "Comment deleted successfully"))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponseDTO>> likeComment(@PathVariable Long id) {
        CommentLikeResponseDTO data = commentService.likeComment(id);
        return ResponseEntity.ok(ApiResponse.<CommentLikeResponseDTO>builder()
                .message(Translator.toLocale("comment.like.success", "Comment liked successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponseDTO>> unlikeComment(@PathVariable Long id) {
        CommentLikeResponseDTO data = commentService.unlikeComment(id);
        return ResponseEntity.ok(ApiResponse.<CommentLikeResponseDTO>builder()
                .message(Translator.toLocale("comment.unlike.success", "Comment unliked successfully"))
                .data(data)
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
