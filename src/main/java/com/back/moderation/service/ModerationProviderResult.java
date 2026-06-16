package com.back.moderation.service;

import java.util.List;

public record ModerationProviderResult(
    double riskScore,
    List<String> categories,
    String reasonCode,
    String reasonMessage
) {}
