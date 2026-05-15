package com.back.block.service;

import com.back.user.model.dto.response.UserInfo;
import com.back.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserBlockService {
    void blockUser(String username);

    void unblockUser(String username);

    Page<UserInfo> getBlockedUsers(Pageable pageable);

    boolean isBlockedEitherWay(User first, User second);

    void assertNotBlockedEitherWay(User first, User second);
}
