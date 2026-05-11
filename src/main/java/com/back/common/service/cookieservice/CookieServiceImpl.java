package com.back.common.service.cookieservice;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CookieServiceImpl implements CookieService {

    @Override
    public String get(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public void add(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // bật nếu dùng HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        response.addHeader("Set-Cookie",
                String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                        name, value, maxAge));

         response.addCookie(cookie);
    }

    @Override
    public void clear(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie",
                String.format("%s=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict",
                        name));
    }
}