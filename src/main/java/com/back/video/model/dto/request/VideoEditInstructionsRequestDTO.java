package com.back.video.model.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoEditInstructionsRequestDTO {
    @Valid
    private VideoTrimRangeRequestDTO videoTrim;

    private Long selectedSoundId;

    @Valid
    private SoundTrimRangeRequestDTO soundTrim;

    @Valid
    private AudioMixSettingsRequestDTO audioMix;

    private Double coverFrameSeconds;
}
