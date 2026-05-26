package com.back.video.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoundTrimRangeRequestDTO {
    @DecimalMin(value = "0.0", message = "startSeconds must be >= 0")
    private Double startSeconds;

    private Double endSeconds;
}
