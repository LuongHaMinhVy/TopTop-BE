package com.back.comment.service;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ICommentService {
    CommentResponseDTO addComment(Long videoId, CommentRequestDTO requestDTO);
    Page<CommentResponseDTO> getCommentsByVideo(Long videoId, Pageable pageable);
    void deleteComment(Long commentId);
}

