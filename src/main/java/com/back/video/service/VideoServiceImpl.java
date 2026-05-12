package com.back.video.service;

import com.back.common.service.R2StorageService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.VideoResponseDTO;
import com.back.video.model.dto.VideoUploadRequestDTO;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {

    private final IVideoRepository videoRepository;
    private final IUserRepo userRepo;
    private final R2StorageService storageService;

    @Override
    public VideoResponseDTO uploadVideo(MultipartFile file, VideoUploadRequestDTO requestDTO) throws IOException {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateVideo(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String key = "video-storage/" + UUID.randomUUID() + extension;
        String url = storageService.uploadFile(file, key);

        Integer duration = null;
        try {
            duration = extractDuration(file);
        } catch (Exception e) {
            log.warn("Failed to extract video duration: {}", e.getMessage());
        }

        Video video = Video.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .category(requestDTO.getCategory())
                .fileUrl(url)
                .duration(duration)
                .user(user)
                .build();

        video = videoRepository.save(video);

        return mapToResponseDTO(video);
    }

    @Override
    public VideoResponseDTO getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        return mapToResponseDTO(video);
    }

    @Override
    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoResponseDTO> getVideosByUserId(Long userId) {
        return videoRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (currentUser == null || !video.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        video.setDeletedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void hardDeleteExpiredVideos() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Video> expired = videoRepository.findExpiredVideos(cutoff);

        for (Video video : expired) {
            try {
                String key = storageService.extractKeyFromUrl(video.getFileUrl());
                storageService.deleteFile(key);
                videoRepository.delete(video);
                log.info("Hard deleted video id={}", video.getId());
            } catch (Exception e) {
                log.error("Failed to hard delete video id={}: {}", video.getId(), e.getMessage());
            }
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return null;
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    private void validateVideo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_IS_REQUIRED);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.INVALID_VIDEO_FILE_TYPE);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ".mp4";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private Integer extractDuration(MultipartFile file) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("video-", ".tmp");
        file.transferTo(tempFile);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    tempFile.toString()
            );
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            if (output.isEmpty()) return null;
            return (int) Double.parseDouble(output);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private VideoResponseDTO mapToResponseDTO(Video video) {
        return VideoResponseDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .fileUrl(video.getFileUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .category(video.getCategory())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .userId(video.getUser().getId())
                .username(video.getUser().getUsername())
                .userNickname(video.getUser().getNickname())
                .userAvatarUrl(video.getUser().getAvatarUrl())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
