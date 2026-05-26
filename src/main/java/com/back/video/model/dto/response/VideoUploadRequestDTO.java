package com.back.video.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadRequestDTO {
    private String title;
    
    private String description;
    
    private String category;

    private String visibility;

    private Boolean allowComments;

    private Boolean allowEdit;

    private Long soundId;

    private Boolean useAvatarAsSoundCover;

    private Boolean enableMusicCopyrightCheck;

    private Boolean enableContentModerationCheck;

    private com.back.video.model.dto.request.VideoEditInstructionsRequestDTO editInstructions;
}
