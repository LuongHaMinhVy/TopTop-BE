package com.back.moderation.service;

import com.back.moderation.model.enums.MusicCopyrightStatus;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.video.model.entity.Video;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "acrcloud.enabled", havingValue = "false", matchIfMissing = true)
public class LocalMusicCopyrightServiceImpl implements IMusicCopyrightService {

    @Override
    public MusicCopyrightResult check(Video video) {
        Sound sound = video.getSound();

        if (sound != null && sound.getType() != SoundType.ORIGINAL
                && Boolean.TRUE.equals(sound.getIsActive())
                && Boolean.TRUE.equals(sound.getIsPublic())) {
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.APPROVED,
                    "PLATFORM_SOUND_LICENSED",
                    "Âm thanh được chọn từ thư viện công khai của hệ thống."
            );
        }

        if (sound != null && sound.getType() == SoundType.ORIGINAL) {
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.NEED_REVIEW,
                    "ORIGINAL_SOUND_REQUIRES_REVIEW",
                    "Âm thanh gốc cần kiểm tra bản quyền thủ công hoặc bằng provider fingerprint."
            );
        }

        return new MusicCopyrightResult(
                MusicCopyrightStatus.NEED_REVIEW,
                "EMBEDDED_AUDIO_REQUIRES_REVIEW",
                "Video có thể chứa âm thanh nhúng cần kiểm tra bản quyền."
        );
    }
}
