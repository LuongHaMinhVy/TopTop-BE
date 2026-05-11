package com.back.common.service.cookieservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CookieService {
    String get(HttpServletRequest request, String name);

    void add(HttpServletResponse response, String name, String value, int maxAge);

    void clear(HttpServletResponse response, String name);
}