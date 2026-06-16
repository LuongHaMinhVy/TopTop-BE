package com.back.sound.service;

import org.springframework.web.multipart.MultipartFile;

public interface IAudioProcessingService {
    String extractAudioUrl(MultipartFile videoFile, String fallbackVideoUrl);
}
