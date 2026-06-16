package com.back.video.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoTrimRangeRequestDTO {
    @DecimalMin(value = "0.0", message = "startSeconds must be >= 0")
    private Double startSeconds;

    @DecimalMin(value = "0.0", message = "endSeconds must be >= 0")
    private Double endSeconds;
}
