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
        String cookieName = getCookieName(request, name);
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    @Override
    public void add(HttpServletResponse response, String name, String value, int maxAge) {
        String cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                name, value, maxAge
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    @Override
    public void add(HttpServletResponse response, String name, String value,
                    int maxAge, HttpServletRequest request) {
        String cookieName = getCookieName(request, name);

        String cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                cookieName, value, maxAge
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    @Override
    public void clear(HttpServletResponse response, String name, HttpServletRequest request) {
        String cookieName = getCookieName(request, name);
        String cookieHeader = String.format(
                "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax",
                cookieName
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private String getCookieName(HttpServletRequest request, String name) {
        if ("JSESSIONID".equals(name)) return name;
        String appId = request.getHeader("X-App-Id");

        if (appId == null || appId.isEmpty()) {
            appId = request.getParameter("X-App-Id");
        }

        if (appId == null || appId.isEmpty()) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("X-App-Id".equals(cookie.getName())) {
                        appId = cookie.getValue();
                        break;
                    }
                }
            }
        }

        return (appId != null && !appId.isEmpty())
                ? name + "_" + appId
                : name;
    }
}