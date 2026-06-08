package com.back.user.model.dto.request;

import lombok.Data;

@Data
public class UpdatePrivacySettingsRequestDTO {
    private Boolean isPrivate;
    private Boolean allowComments;
    private Boolean allowMessageFromEveryone;
    private Boolean showPosts;
    private Boolean showReposts;
    private Boolean showLikedVideos;
    private Boolean showFavorites;
}
