package com.back.sound.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.sound.mapper.SoundMapper;
import com.back.sound.model.dto.request.CreateSoundRequestDTO;
import com.back.sound.model.dto.request.UpdateSoundRequestDTO;
import com.back.sound.model.dto.response.SoundDetailResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.dto.response.SoundStatsResponseDTO;
import com.back.sound.model.entity.SavedSound;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.sound.repo.ISavedSoundRepository;
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
    private final ISavedSoundRepository savedSoundRepository;
    private final IVideoRepository videoRepository;
    private final IVideoService videoService;
    private final IUserRepo userRepo;
    private final SoundMapper soundMapper;

    @Override
    public Page<SoundResponseDTO> searchPublicSounds(String keyword, SoundType type, Pageable pageable) {
        User currentUser = getCurrentUserOrNull();
        Page<Sound> soundPage = soundRepository.searchPublicSounds(normalizeKeyword(keyword), type, pageable);
        if (currentUser == null || soundPage.isEmpty()) {
            return soundPage.map(soundMapper::toResponseDTO);
        }

        var soundIds = soundPage.getContent().stream().map(Sound::getId).toList();
        var savedIds = savedSoundRepository.findSavedSoundIdsByUser(currentUser.getId(), soundIds);
        return soundPage.map(sound -> soundMapper.toResponseDTO(sound, savedIds.contains(sound.getId())));
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
        boolean isSaved = currentUser != null
                && savedSoundRepository.existsByUserIdAndSoundId(currentUser.getId(), sound.getId());

        return soundMapper.toDetailDTO(sound, true, canEdit, isSaved);
    }

    @Override
    public Page<VideoResponseDTO> getSoundVideos(Long soundId, Pageable pageable) {
        getPublicSound(soundId);
        return videoRepository.findPublicVideosBySoundId(soundId, pageable)
                .map(video -> videoService.getVideoById(video.getId()));
    }

    @Override
    public Page<SoundResponseDTO> getFavoriteSounds(Pageable pageable) {
        User currentUser = getCurrentUserOrThrow();
        return savedSoundRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(savedSound -> soundMapper.toResponseDTO(savedSound.getSound(), true));
    }

    @Override
    @Transactional
    public SoundStatsResponseDTO saveSound(Long soundId) {
        User currentUser = getCurrentUserOrThrow();
        Sound sound = getPublicSound(soundId);

        if (!savedSoundRepository.existsByUserIdAndSoundId(currentUser.getId(), soundId)) {
            SavedSound savedSound = SavedSound.builder()
                    .user(currentUser)
                    .sound(sound)
                    .build();
            savedSoundRepository.save(savedSound);
            soundRepository.incrementSavedCount(soundId);
            sound.setSavedCount((sound.getSavedCount() == null ? 0L : sound.getSavedCount()) + 1);
        }

        return soundMapper.toStatsDTO(sound, true);
    }

    @Override
    @Transactional
    public SoundStatsResponseDTO unsaveSound(Long soundId) {
        User currentUser = getCurrentUserOrThrow();
        Sound sound = getPublicSound(soundId);

        savedSoundRepository.findByUserIdAndSoundId(currentUser.getId(), soundId).ifPresent(savedSound -> {
            savedSoundRepository.delete(savedSound);
            soundRepository.decrementSavedCount(soundId);
            sound.setSavedCount(Math.max(0L, sound.getSavedCount() == null ? 0L : sound.getSavedCount() - 1));
        });

        return soundMapper.toStatsDTO(sound, false);
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
                .savedCount(0L)
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

    private User getCurrentUserOrThrow() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
