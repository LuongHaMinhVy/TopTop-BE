package com.back.comment.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDTO {
    @NotBlank(message = "comment.content.required")
    @Size(max = 2000, message = "comment.content.max")
    private String content;

    private Long parentId;

    @Min(value = 0, message = "comment.timestamp.invalid")
    private Integer timestampInVideo;
}
