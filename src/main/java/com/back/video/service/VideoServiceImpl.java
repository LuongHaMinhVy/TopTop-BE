package com.back.video.service;

import com.back.notification.service.INotificationService;

import com.back.common.service.R2StorageService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoUploadRequestDTO;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import com.back.hashtag.repo.IHashtagRepository;
import com.back.hashtag.model.entity.Hashtag;
import com.back.video.model.enums.VideoVisibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {

    private final IVideoRepository videoRepository;
    private final IUserRepo userRepo;
    private final R2StorageService storageService;
    private final IHashtagRepository hashtagRepo;

    @Override
    public VideoResponseDTO uploadVideo(MultipartFile file, MultipartFile cover, VideoUploadRequestDTO requestDTO) throws IOException {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        validateVideo(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String key = "video-storage/" + UUID.randomUUID() + extension;
        String url = storageService.uploadFile(file, key);

        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            String coverExtension = getFileExtension(cover.getOriginalFilename());
            String coverKey = "video-storage/covers/" + UUID.randomUUID() + coverExtension;
            coverUrl = storageService.uploadFile(cover, coverKey);
        }

        Integer duration = null;
        try {
            duration = extractDuration(file);
        } catch (Exception e) {
            log.warn("Failed to extract video duration: {}", e.getMessage());
        }

        java.util.Set<Hashtag> extractedHashtags = new java.util.HashSet<>();
        if (requestDTO.getDescription() != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#(\\w+)").matcher(requestDTO.getDescription());
            while (matcher.find()) {
                String hashtagName = matcher.group(1).toLowerCase();
                Hashtag hashtag = hashtagRepo.findByName(hashtagName)
                        .orElseGet(() -> hashtagRepo.save(Hashtag.builder().name(hashtagName).postCount(0L).build()));
                hashtag.setPostCount(hashtag.getPostCount() + 1);
                hashtagRepo.save(hashtag);
                extractedHashtags.add(hashtag);
            }
        }

        Video video = Video.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .category(requestDTO.getCategory())
                .fileUrl(url)
                .thumbnailUrl(coverUrl)
                .duration(duration)
                .user(user)
                .hashtags(extractedHashtags)
                .visibility(requestDTO.getVisibility() != null ? VideoVisibility.valueOf(requestDTO.getVisibility()) : VideoVisibility.PUBLIC)
                .allowComments(requestDTO.getAllowComments() != null ? requestDTO.getAllowComments() : true)
                .allowEdit(requestDTO.getAllowEdit() != null ? requestDTO.getAllowEdit() : false)
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
    public Page<VideoResponseDTO> getAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public Page<VideoResponseDTO> getVideosByUserId(Long userId, Pageable pageable) {
        return videoRepository.findByUserId(userId, pageable)
                .map(this::mapToResponseDTO);
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

    @Override
    public void reportVideo(Long id, String reason) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        log.info("Video {} reported for reason: {}", id, reason);
        // In a real app, we would save this to a Report entity
    }

    private final INotificationService notificationService;

    @Override
    public void likeVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        
        User currentUser = getCurrentUser();
        video.setLikeCount(video.getLikeCount() + 1);
        videoRepository.save(video);

        if (currentUser != null) {
            notificationService.createNotification(
                    video.getUser(),
                    currentUser,
                    video,
                    "LIKE",
                    currentUser.getUsername() + " liked your video: " + video.getTitle()
            );
        }
    }

    @Override
    public void unlikeVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (video.getLikeCount() > 0) {
            video.setLikeCount(video.getLikeCount() - 1);
            videoRepository.save(video);
        }
    }

    @Override
    public Page<VideoResponseDTO> getFavoriteVideos(Pageable pageable) {
        // Return dummy favorites for now, e.g. all videos or empty list
        // In a real app, query from Collections/Favorites table for the current user
        return videoRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public VideoResponseDTO getVideoByUsernameAndId(String username, Long videoId) {
        Video video = videoRepository.findByUserUsernameAndId(username, videoId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        
        // Increment view count
        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);
        
        return mapToResponseDTO(video);
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
