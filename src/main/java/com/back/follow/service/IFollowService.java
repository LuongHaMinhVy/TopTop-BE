package com.back.follow.service;

import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.entity.User;

public interface IFollowService {
    void followUser(String targetUsername);
    void unfollowUser(String targetUsername);
    RelationshipStatus getRelationshipStatus(User currentUser, User targetUser);
}
