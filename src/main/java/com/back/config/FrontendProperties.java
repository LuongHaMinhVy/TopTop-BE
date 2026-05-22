package com.back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "frontend")
public class FrontendProperties {

    private List<String> urls = new ArrayList<>();

    public List<String> getUrls() {
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .map(this::removeTrailingSlash)
                .toList();
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getPrimaryUrl() {
        List<String> configuredUrls = getUrls();
        if (configuredUrls.isEmpty()) {
            throw new IllegalStateException("frontend.urls must contain at least one URL");
        }
        return configuredUrls.getFirst();
    }

    public boolean isAllowedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalizedUrl = removeTrailingSlash(url.trim());
        return getUrls().contains(normalizedUrl);
    }

    private String removeTrailingSlash(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
