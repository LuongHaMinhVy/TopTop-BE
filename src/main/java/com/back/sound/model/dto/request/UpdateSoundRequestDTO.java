package com.back.sound.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSoundRequestDTO {
    @Size(max = 150, message = "sound.title.max")
    private String title;

    @Size(max = 150, message = "sound.artistName.max")
    private String artistName;

    @Size(max = 500, message = "sound.description.max")
    private String description;

    private String coverUrl;
    private Integer durationSeconds;
    private Boolean isPublic;
    private Boolean isActive;
}
