package com.back.sound.service;

import com.back.sound.model.dto.request.CreateSoundRequestDTO;
import com.back.sound.model.dto.request.UpdateSoundRequestDTO;
import com.back.sound.model.dto.response.SoundDetailResponseDTO;
import com.back.sound.model.dto.response.SoundResponseDTO;
import com.back.sound.model.dto.response.SoundStatsResponseDTO;
import com.back.sound.model.enums.SoundType;
import com.back.video.model.dto.request.VideoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISoundService {
    Page<SoundResponseDTO> searchPublicSounds(String keyword, SoundType type, Pageable pageable);

    Page<SoundResponseDTO> searchAdminSounds(String keyword, Pageable pageable);

    SoundDetailResponseDTO getSoundDetail(Long soundId);

    Page<VideoResponseDTO> getSoundVideos(Long soundId, Pageable pageable);

    Page<SoundResponseDTO> getFavoriteSounds(Pageable pageable);

    SoundStatsResponseDTO saveSound(Long soundId);

    SoundStatsResponseDTO unsaveSound(Long soundId);

    SoundResponseDTO createSound(CreateSoundRequestDTO requestDTO);

    SoundResponseDTO updateSound(Long soundId, UpdateSoundRequestDTO requestDTO);

    void deleteSound(Long soundId);
}
