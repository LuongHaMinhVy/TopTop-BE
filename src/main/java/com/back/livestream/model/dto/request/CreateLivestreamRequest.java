package com.back.livestream.model.dto.request;

import com.back.livestream.model.enums.LivestreamVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLivestreamRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    private Long categoryId;

    private String thumbnailUrl;

    private LivestreamVisibility visibility = LivestreamVisibility.PUBLIC;

    private boolean allowChat = true;

    private boolean allowGifts = true;
}
