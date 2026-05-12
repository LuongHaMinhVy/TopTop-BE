package com.back.video.service;

import com.back.video.model.dto.VideoResponseDTO;
import com.back.video.model.dto.VideoUploadRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IVideoService {
    VideoResponseDTO uploadVideo(MultipartFile file, VideoUploadRequestDTO requestDTO) throws IOException;
    VideoResponseDTO getVideoById(Long id);
    List<VideoResponseDTO> getAllVideos();
    List<VideoResponseDTO> getVideosByUserId(Long userId);
    void deleteVideo(Long id);
}
