package com.back.follow.service;

import com.back.user.model.dto.response.RelationshipStatus;
import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface IFollowService {
    void followUser(String username);
    void unfollowUser(String username);
    Page<UserInfo> getFollowingList(Pageable pageable);


    RelationshipStatus getRelationshipStatus(User currentUser, User targetUser);
}
