package com.back.moderation.service;

import java.util.List;

/**
 * Result of analysing a single video frame for quality/policy issues.
 *
 * @param riskScore      0.0 – 1.0 safety risk score (from Gemini)
 * @param categories     safety policy categories (SEXUAL, VIOLENCE …)
 * @param qualityIssues  quality / originality issues (WATERMARK, QR_CODE, LOW_QUALITY)
 * @param reasonCode     machine-readable issue code, may be null
 * @param reasonMessage  human-readable Vietnamese message, may be null
 */
public record VideoFrameAnalysisResult(
        double riskScore,
        List<String> categories,
        List<String> qualityIssues,
        String reasonCode,
        String reasonMessage
) {}
