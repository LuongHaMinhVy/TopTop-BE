package com.back.comment.service;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import java.util.List;

public interface ICommentService {
    CommentResponseDTO addComment(Long videoId, CommentRequestDTO requestDTO);
    List<CommentResponseDTO> getCommentsByVideo(Long videoId);
    void deleteComment(Long commentId);
}
