package com.back.comment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeResponseDTO {
    private Long commentId;
    private Boolean liked;
    private Long likeCount;
}
