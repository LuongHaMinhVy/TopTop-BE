package com.back.sound.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.sound.mapper.SoundMapper;
import com.back.sound.model.dto.request.CreateSoundRequestDTO;
import com.back.sound.model.dto.request.UpdateSoundRequestDTO;
import com.back.sound.model.dto.response.SoundDetailResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.sound.repo.ISoundRepository;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.repo.IVideoRepository;
import com.back.video.service.IVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SoundServiceImpl implements ISoundService {

    private final ISoundRepository soundRepository;
    private final IVideoRepository videoRepository;
    private final IVideoService videoService;
    private final IUserRepo userRepo;
    private final SoundMapper soundMapper;

    @Override
    public Page<SoundResponseDTO> searchPublicSounds(String keyword, SoundType type, Pageable pageable) {
        return soundRepository.searchPublicSounds(normalizeKeyword(keyword), type, pageable)
                .map(soundMapper::toResponseDTO);
    }

    @Override
    public Page<SoundResponseDTO> searchAdminSounds(String keyword, Pageable pageable) {
        return soundRepository.searchAdminSounds(normalizeKeyword(keyword), pageable)
                .map(soundMapper::toResponseDTO);
    }

    @Override
    public SoundDetailResponseDTO getSoundDetail(Long soundId) {
        Sound sound = getPublicSound(soundId);
        User currentUser = getCurrentUserOrNull();
        boolean canEdit = currentUser != null
                && sound.getOwner() != null
                && sound.getOwner().getId().equals(currentUser.getId());

        return soundMapper.toDetailDTO(sound, true, canEdit);
    }

    @Override
    public Page<VideoResponseDTO> getSoundVideos(Long soundId, Pageable pageable) {
        getPublicSound(soundId);
        return videoRepository.findPublicVideosBySoundId(soundId, pageable)
                .map(video -> videoService.getVideoById(video.getId()));
    }

    @Override
    @Transactional
    public SoundResponseDTO createSound(CreateSoundRequestDTO requestDTO) {
        Sound sound = Sound.builder()
                .title(requestDTO.getTitle().trim())
                .artistName(requestDTO.getArtistName())
                .description(requestDTO.getDescription())
                .type(requestDTO.getType())
                .audioUrl(requestDTO.getAudioUrl())
                .coverUrl(requestDTO.getCoverUrl())
                .durationSeconds(requestDTO.getDurationSeconds() == null ? 0 : requestDTO.getDurationSeconds())
                .isPublic(requestDTO.getIsPublic() == null ? true : requestDTO.getIsPublic())
                .isActive(true)
                .isDeleted(false)
                .usageCount(0L)
                .build();

        return soundMapper.toResponseDTO(soundRepository.save(sound));
    }

    @Override
    @Transactional
    public SoundResponseDTO updateSound(Long soundId, UpdateSoundRequestDTO requestDTO) {
        Sound sound = soundRepository.findByIdAndIsDeletedFalse(soundId)
                .orElseThrow(() -> new AppException(ErrorCode.SOUND_NOT_FOUND));

        if (requestDTO.getTitle() != null) sound.setTitle(requestDTO.getTitle().trim());
        if (requestDTO.getArtistName() != null) sound.setArtistName(requestDTO.getArtistName());
        if (requestDTO.getDescription() != null) sound.setDescription(requestDTO.getDescription());
        if (requestDTO.getCoverUrl() != null) sound.setCoverUrl(requestDTO.getCoverUrl());
        if (requestDTO.getDurationSeconds() != null) sound.setDurationSeconds(requestDTO.getDurationSeconds());
        if (requestDTO.getIsPublic() != null) sound.setIsPublic(requestDTO.getIsPublic());
        if (requestDTO.getIsActive() != null) sound.setIsActive(requestDTO.getIsActive());

        return soundMapper.toResponseDTO(soundRepository.save(sound));
    }

    @Override
    @Transactional
    public void deleteSound(Long soundId) {
        Sound sound = soundRepository.findByIdAndIsDeletedFalse(soundId)
                .orElseThrow(() -> new AppException(ErrorCode.SOUND_NOT_FOUND));
        sound.setIsDeleted(true);
        sound.setIsActive(false);
        soundRepository.save(sound);
    }

    private Sound getPublicSound(Long soundId) {
        Sound sound = soundRepository.findByIdAndIsDeletedFalse(soundId)
                .orElseThrow(() -> new AppException(ErrorCode.SOUND_NOT_FOUND));
        if (!Boolean.TRUE.equals(sound.getIsPublic()) || !Boolean.TRUE.equals(sound.getIsActive())) {
            throw new AppException(ErrorCode.SOUND_NOT_FOUND);
        }
        return sound;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        return keyword.trim();
    }

    private User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email).orElse(null);
    }
}
