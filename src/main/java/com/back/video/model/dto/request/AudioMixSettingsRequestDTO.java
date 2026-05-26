package com.back.video.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioMixSettingsRequestDTO {
    @DecimalMin(value = "0.0", message = "originalAudioVolume must be >= 0")
    @DecimalMax(value = "1.0", message = "originalAudioVolume must be <= 1")
    private Double originalAudioVolume;

    @DecimalMin(value = "0.0", message = "soundVolume must be >= 0")
    @DecimalMax(value = "1.0", message = "soundVolume must be <= 1")
    private Double soundVolume;

    @DecimalMin(value = "0.0", message = "soundStartAtVideoSeconds must be >= 0")
    private Double soundStartAtVideoSeconds;
}
