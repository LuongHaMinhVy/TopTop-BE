package com.back.comment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private Long videoId;
    private Long parentId;
    private LocalDateTime createdAt;
}
