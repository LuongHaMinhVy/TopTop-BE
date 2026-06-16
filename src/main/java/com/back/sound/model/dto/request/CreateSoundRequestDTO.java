package com.back.sound.model.dto.request;

import com.back.sound.model.enums.SoundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSoundRequestDTO {
    @NotBlank(message = "sound.title.required")
    @Size(max = 150, message = "sound.title.max")
    private String title;

    @Size(max = 150, message = "sound.artistName.max")
    private String artistName;

    @Size(max = 500, message = "sound.description.max")
    private String description;

    @NotNull(message = "sound.type.required")
    private SoundType type;

    @NotBlank(message = "sound.audio.required")
    private String audioUrl;

    private String coverUrl;
    private Integer durationSeconds;
    private Boolean isPublic;
}
