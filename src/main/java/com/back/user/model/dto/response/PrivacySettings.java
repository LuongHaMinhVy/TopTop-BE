package com.back.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class PrivacySettings {
        private Boolean allowComments;
        private Boolean allowDuet;
        private Boolean allowStitch;
        private Boolean allowDownload;
        private Boolean allowMessageFromEveryone;
        private Boolean showPosts;
        private Boolean showReposts;
        private Boolean showLikedVideos;
        private Boolean showFavorites;
        private String defaultCommentPermission;
        private String messagePrivacy;
    }
