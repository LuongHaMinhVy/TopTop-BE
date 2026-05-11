package com.back.user.service;

import com.back.user.model.dto.response.UserInfo;

import jakarta.servlet.http.HttpServletRequest;

public interface IUserService{
    UserInfo getUserInfo(HttpServletRequest request);
}
