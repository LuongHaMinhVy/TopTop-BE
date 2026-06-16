package com.back.sound.mapper;

import com.back.sound.model.dto.response.SoundAuthorResponseDTO;
import com.back.sound.model.dto.response.SoundDetailResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.dto.response.SoundStatsResponseDTO;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.user.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class SoundMapper {

    public SoundResponseDTO toResponseDTO(Sound sound) {
        return toResponseDTO(sound, false);
    }

    public SoundResponseDTO toResponseDTO(Sound sound, boolean isSaved) {
        if (sound == null) return null;

        return SoundResponseDTO.builder()
                .id(sound.getId())
                .title(sound.getTitle())
                .artistName(sound.getArtistName())
                .audioUrl(sound.getAudioUrl())
                .coverUrl(sound.getCoverUrl())
                .durationSeconds(sound.getDurationSeconds())
                .type(sound.getType().name())
                .originalSound(sound.getType() == SoundType.ORIGINAL)
                .owner(toAuthorDTO(sound.getOwner()))
                .stats(toStatsDTO(sound, isSaved))
                .isSaved(isSaved)
                .isPublic(sound.getIsPublic())
                .isActive(sound.getIsActive())
                .createdAt(sound.getCreatedAt())
                .build();
    }

    public SoundDetailResponseDTO toDetailDTO(Sound sound, boolean canUse, boolean canEdit) {
        return toDetailDTO(sound, canUse, canEdit, false);
    }

    public SoundDetailResponseDTO toDetailDTO(Sound sound, boolean canUse, boolean canEdit, boolean isSaved) {
        if (sound == null) return null;

        return SoundDetailResponseDTO.builder()
                .id(sound.getId())
                .title(sound.getTitle())
                .artistName(sound.getArtistName())
                .description(sound.getDescription())
                .audioUrl(sound.getAudioUrl())
                .coverUrl(sound.getCoverUrl())
                .durationSeconds(sound.getDurationSeconds())
                .type(sound.getType().name())
                .originalSound(sound.getType() == SoundType.ORIGINAL)
                .owner(toAuthorDTO(sound.getOwner()))
                .sourceVideoId(sound.getSourceVideo() != null ? sound.getSourceVideo().getId() : null)
                .stats(toStatsDTO(sound, isSaved))
                .isSaved(isSaved)
                .canUse(canUse)
                .canEdit(canEdit)
                .isPublic(sound.getIsPublic())
                .isActive(sound.getIsActive())
                .createdAt(sound.getCreatedAt())
                .updatedAt(sound.getUpdatedAt())
                .build();
    }

    public SoundStatsResponseDTO toStatsDTO(Sound sound, boolean isSaved) {
        long usageCount = sound.getUsageCount() == null ? 0L : sound.getUsageCount();
        long savedCount = sound.getSavedCount() == null ? 0L : sound.getSavedCount();
        return SoundStatsResponseDTO.builder()
                .soundId(sound.getId())
                .usageCount(usageCount)
                .videoCount(usageCount)
                .savedCount(savedCount)
                .isSaved(isSaved)
                .build();
    }

    private SoundAuthorResponseDTO toAuthorDTO(User user) {
        if (user == null) return null;

        return SoundAuthorResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.getVerified())
                .build();
    }
}
