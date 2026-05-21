package com.back.sound.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FfmpegAudioProcessingServiceImpl implements IAudioProcessingService {

    @Override
    public String extractAudioUrl(MultipartFile videoFile, String fallbackVideoUrl) {
        // Phase 1 fallback: keep a playable URL without blocking upload when FFmpeg/audio
        // storage is not configured in the local environment.
        log.debug("Using video URL as original sound fallback for {}", videoFile.getOriginalFilename());
        return fallbackVideoUrl;
    }
}
