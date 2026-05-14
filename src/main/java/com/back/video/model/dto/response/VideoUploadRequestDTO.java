package com.back.video.model.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String category;

    private String visibility;

    private Boolean allowComments;

    private Boolean allowEdit;
}
