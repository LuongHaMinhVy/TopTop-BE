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
    public class RelationshipStatus {
        private Boolean isFollowing;
        private Boolean isFollower;
        private Boolean isBlocked;
        private Boolean isBlockedBy;
        private Boolean isFriend;
    }
