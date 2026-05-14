package com.back.video.service;

import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoUploadRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IVideoService {
    VideoResponseDTO uploadVideo(MultipartFile file, MultipartFile cover, VideoUploadRequestDTO requestDTO) throws IOException;
    VideoResponseDTO getVideoById(Long id);
    Page<VideoResponseDTO> getAllVideos(Pageable pageable);
    Page<VideoResponseDTO> getVideosByUserId(Long userId, Pageable pageable);
    void deleteVideo(Long id);
    void reportVideo(Long id, String reason);
    void likeVideo(Long id);
    void unlikeVideo(Long id);
    Page<VideoResponseDTO> getFavoriteVideos(Pageable pageable);
    VideoResponseDTO getVideoByUsernameAndId(String username, Long videoId);
}
