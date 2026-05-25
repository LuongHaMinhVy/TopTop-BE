package com.back.moderation.service;

import com.back.moderation.model.enums.MusicCopyrightStatus;

public record MusicCopyrightResult(
        MusicCopyrightStatus status,
        String reasonCode,
        String reasonMessage
) {}
