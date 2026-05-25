package com.back.moderation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Free fallback moderation provider using keyword blocklists and basic metadata rules.
 * Cannot detect NSFW/violence from raw images; sets NEED_REVIEW for suspicious text.
 */
@Slf4j
@Component
public class LocalRulesModerationProvider implements IModerationProvider {

    private final Set<String> blockedKeywords;

    public LocalRulesModerationProvider() {
        this.blockedKeywords = loadBlocklist();
    }

    private Set<String> loadBlocklist() {
        Set<String> keywords = new java.util.HashSet<>();
        keywords.addAll(loadFromClasspath("moderation/blocklist_vi.txt"));
        keywords.addAll(loadFromClasspath("moderation/blocklist_en.txt"));
        return keywords;
    }

    private Set<String> loadFromClasspath(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) return Set.of();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Could not load blocklist from {}: {}", path, e.getMessage());
            return Set.of();
        }
    }

    @Override
    public ModerationProviderResult moderateText(TextModerationInput input) {
        List<String> categories = new ArrayList<>();
        double riskScore = 0.0;
        String reasonCode = null;

        String text = buildTextToCheck(input);

        // Check against blocklist
        for (String keyword : blockedKeywords) {
            if (text.contains(keyword)) {
                riskScore = Math.max(riskScore, 0.75);
                categories.add("BLOCKED_KEYWORD");
                reasonCode = "BLOCKED_KEYWORD_DETECTED";
                break;
            }
        }

        // Hashtag spam check (> 20 hashtags)
        if (input.hashtags() != null && input.hashtags().size() > 20) {
            riskScore = Math.max(riskScore, 0.5);
            categories.add("HASHTAG_SPAM");
            if (reasonCode == null) reasonCode = "HASHTAG_SPAM";
        }

        // Link spam check
        if (text.matches(".*https?://\\S+.*") && input.caption() != null && input.caption().split("https?://").length > 3) {
            riskScore = Math.max(riskScore, 0.6);
            categories.add("LINK_SPAM");
            if (reasonCode == null) reasonCode = "LINK_SPAM";
        }

        return new ModerationProviderResult(riskScore, categories, reasonCode,
                reasonCode != null ? "Nội dung vi phạm phát hiện bởi kiểm tra nội bộ." : null);
    }

    @Override
    public ModerationProviderResult moderateImage(ImageModerationInput input) {
        // Local rules cannot detect NSFW/violence from frames without AI.
        // Return low risk, let admin review if needed.
        return new ModerationProviderResult(0.0, List.of(), null, null);
    }

    private String buildTextToCheck(TextModerationInput input) {
        StringBuilder sb = new StringBuilder();
        if (input.caption() != null) sb.append(input.caption().toLowerCase()).append(" ");
        if (input.hashtags() != null) input.hashtags().forEach(h -> sb.append(h.toLowerCase()).append(" "));
        return sb.toString();
    }
}
