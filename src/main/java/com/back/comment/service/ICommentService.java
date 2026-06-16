package com.back.comment.service;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentLikeResponseDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICommentService {
    CommentResponseDTO addComment(Long videoId, CommentRequestDTO requestDTO);
    Page<CommentResponseDTO> getCommentsByVideo(Long videoId, Pageable pageable);
    Page<CommentResponseDTO> getReplies(Long commentId, Pageable pageable);
    CommentResponseDTO addReply(Long commentId, CommentRequestDTO requestDTO);
    void deleteComment(Long commentId);
    CommentLikeResponseDTO likeComment(Long commentId);
    CommentLikeResponseDTO unlikeComment(Long commentId);
}
