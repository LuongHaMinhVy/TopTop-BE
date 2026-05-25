package com.back.moderation.service;

public interface ITextContentModerationService {
    void assertAllowed(String targetType, String text, Long actorUserId, String fieldName);
}
