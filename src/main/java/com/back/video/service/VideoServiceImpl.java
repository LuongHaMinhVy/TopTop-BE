package com.back.video.service;

import com.back.collection.repo.ISavedVideoRepository;
import com.back.collection.repo.ICollectionVideoRepository;
import com.back.chat.repo.IMessageAttachmentRepository;
import com.back.notification.service.INotificationService;

import com.back.block.service.IUserBlockService;
import com.back.common.service.R2StorageService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.model.enums.MusicCopyrightStatus;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.sound.mapper.SoundMapper;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.sound.repo.ISoundRepository;
import com.back.sound.service.IAudioProcessingService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoDailyMetricResponseDTO;
import com.back.video.model.dto.response.VideoRepostUserResponseDTO;
import com.back.video.model.dto.response.VideoStatsResponseDTO;
import com.back.video.model.dto.response.VideoUploadRequestDTO;
import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoLike;
import com.back.video.model.entity.VideoRepost;
import com.back.video.model.entity.VideoView;
import com.back.video.repo.IVideoLikeRepository;
import com.back.video.repo.IVideoRepository;
import com.back.video.repo.IVideoRepostRepository;
import com.back.video.repo.IVideoViewRepository;
import com.back.hashtag.repo.IHashtagRepository;
import com.back.hashtag.model.entity.Hashtag;
import com.back.video.model.enums.VideoVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.redis.core.RedisTemplate;
import com.back.video.model.dto.request.InitVideoUploadRequestDTO;
import com.back.video.model.dto.request.CompleteVideoUploadRequestDTO;
import com.back.video.model.dto.response.InitVideoUploadResponseDTO;

import com.back.moderation.service.IVideoModerationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {

    private final IVideoRepository videoRepository;
    private final IVideoLikeRepository videoLikeRepository;
    private final IVideoRepostRepository videoRepostRepository;
    private final IUserRepo userRepo;
    private final R2StorageService storageService;
    private final IHashtagRepository hashtagRepo;
    private final ISavedVideoRepository ISavedVideoRepository;
    private final ICollectionVideoRepository ICollectionVideoRepository;
    private final IUserBlockService userBlockService;
    private final com.back.follow.repo.IFollowRepo followRepo;
    private final ISoundRepository soundRepository;
    private final SoundMapper soundMapper;
    private final IAudioProcessingService audioProcessingService;
    private final IMessageAttachmentRepository messageAttachmentRepository;
    private final IVideoViewRepository videoViewRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoDeletionService videoDeletionService;
    private IVideoModerationService videoModerationService;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setVideoModerationService(IVideoModerationService videoModerationService) {
        this.videoModerationService = videoModerationService;
    }

    @Override
    public InitVideoUploadResponseDTO initVideoUpload(InitVideoUploadRequestDTO requestDTO) {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!requestDTO.getContentType().startsWith("video/")) {
            throw new AppException(ErrorCode.INVALID_VIDEO_FILE_TYPE);
        }

        String extension = getFileExtension(requestDTO.getFileName());
        String uploadId = UUID.randomUUID().toString();
        String objectKey = "video-storage/" + uploadId + extension;
        String uploadUrl = storageService.generatePresignedUploadUrl(objectKey, requestDTO.getContentType(), java.time.Duration.ofMinutes(30));

        redisTemplate.opsForValue().set("upload:" + uploadId, objectKey, 30, java.util.concurrent.TimeUnit.MINUTES);

        return InitVideoUploadResponseDTO.builder()
                .uploadId(uploadId)
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .method("PUT")
                .build();
    }

    @Override
    public VideoResponseDTO completeVideoUpload(CompleteVideoUploadRequestDTO requestDTO, MultipartFile cover) {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (requestDTO.getTitle() == null || requestDTO.getTitle().trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String objectKey = (String) redisTemplate.opsForValue().get("upload:" + requestDTO.getUploadId());
        if (objectKey == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String fileUrl = storageService.buildPublicUrl(objectKey);

        String coverUrl = null;
        if (cover != null && !cover.isEmpty()) {
            String coverExtension = getFileExtension(cover.getOriginalFilename());
            String coverKey = "video-storage/covers/" + UUID.randomUUID() + coverExtension;
            try {
                coverUrl = storageService.uploadFile(cover, coverKey);
            } catch (IOException e) {
                log.warn("Cover upload failed", e);
            }
        }

        Integer duration = 0; // Duration extraction requires file, deferred to processing worker if any

        Set<Hashtag> extractedHashtags = new HashSet<>();
        if (requestDTO.getDescription() != null) {
            Matcher matcher = Pattern.compile("#(\\w+)").matcher(requestDTO.getDescription());
            while (matcher.find()) {
                String hashtagName = matcher.group(1).toLowerCase();
                Hashtag hashtag = hashtagRepo.findByName(hashtagName)
                        .orElseGet(() -> hashtagRepo.save(Hashtag.builder().name(hashtagName).postCount(0L).build()));
                hashtag.setPostCount(hashtag.getPostCount() + 1);
                hashtagRepo.save(hashtag);
                extractedHashtags.add(hashtag);
            }
        }

        Sound selectedSound = resolveSelectedSound(requestDTO.getSoundId());

        Video video = Video.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .category(requestDTO.getCategory())
                .fileUrl(fileUrl)
                .thumbnailUrl(coverUrl)
                .duration(duration)
                .user(user)
                .hashtags(extractedHashtags)
                .visibility(requestDTO.getVisibility() != null ? VideoVisibility.valueOf(requestDTO.getVisibility()) : VideoVisibility.PUBLIC)
                .allowComments(requestDTO.getAllowComments() != null ? requestDTO.getAllowComments() : true)
                .allowEdit(requestDTO.getAllowEdit() != null ? requestDTO.getAllowEdit() : false)
                .sound(selectedSound)
                .build();

        video = videoRepository.save(video);

        if (selectedSound != null) {
            soundRepository.incrementUsageCount(selectedSound.getId());
        }

        redisTemplate.delete("upload:" + requestDTO.getUploadId());

        // Trigger async moderation
        final Long videoId = video.getId();
        videoModerationService.runModeration(videoId);

        return mapToResponseDTO(video);
    }

    @Override
    public VideoResponseDTO uploadVideo(MultipartFile file, MultipartFile cover, VideoUploadRequestDTO requestDTO) throws IOException {
        User user = getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (requestDTO.getTitle() == null || requestDTO.getTitle().trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
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

        Set<Hashtag> extractedHashtags = new HashSet<>();
        if (requestDTO.getDescription() != null) {
            Matcher matcher = Pattern.compile("#(\\w+)").matcher(requestDTO.getDescription());
            while (matcher.find()) {
                String hashtagName = matcher.group(1).toLowerCase();
                Hashtag hashtag = hashtagRepo.findByName(hashtagName)
                        .orElseGet(() -> hashtagRepo.save(Hashtag.builder().name(hashtagName).postCount(0L).build()));
                hashtag.setPostCount(hashtag.getPostCount() + 1);
                hashtagRepo.save(hashtag);
                extractedHashtags.add(hashtag);
            }
        }

        Sound selectedSound = resolveSelectedSound(requestDTO.getSoundId());

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
                .sound(selectedSound)
                .build();

        video = videoRepository.save(video);

        if (selectedSound == null) {
            Sound originalSound = createOriginalSound(video, user, file, url, duration, requestDTO);
            video.setSound(originalSound);
            video = videoRepository.save(video);
        } else {
            soundRepository.incrementUsageCount(selectedSound.getId());
        }

        return mapToResponseDTO(video);
    }

    @Override
    public VideoResponseDTO getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }
        User currentUser = getCurrentUser();
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);
        return mapToResponseDTO(video);
    }

    @Override
    public Page<VideoResponseDTO> getAllVideos(Pageable pageable) {
        User currentUser = getCurrentUser();
        Long viewerId = currentUser == null ? null : currentUser.getId();
        
        Pageable candidatePageable = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Video> candidates = new java.util.ArrayList<>(videoRepository.findAllVisibleForViewer(viewerId, candidatePageable).getContent());
        
        candidates.sort((v1, v2) -> Double.compare(calculateForYouScore(v2, currentUser), calculateForYouScore(v1, currentUser)));
        
        return paginateList(candidates, pageable).map(this::mapToResponseDTO);
    }

    @Override
    public Page<VideoResponseDTO> getVideosByUserId(Long userId, Pageable pageable) {
        User currentUser = getCurrentUser();
        return videoRepository.findByUserIdVisibleForViewer(
                        userId,
                        currentUser == null ? null : currentUser.getId(),
                        pageable)
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
        if (video.getSound() != null) {
            soundRepository.decrementUsageCount(video.getSound().getId());
        }
        videoRepository.save(video);
    }

    @Override
    public void reportVideo(Long id, String reason) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }
        userBlockService.assertNotBlockedEitherWay(getCurrentUser(), video.getUser());
        log.info("Video {} reported for reason: {}", id, reason);
    }

    private final INotificationService notificationService;

    @Override
    public VideoStatsResponseDTO likeVideo(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());

        if (videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id)) {
            return mapToStatsDTO(video, true);
        }

        VideoLike like = VideoLike.builder()
                .user(currentUser)
                .video(video)
                .build();
        videoLikeRepository.save(like);

        video.setLikeCount(safe(video.getLikeCount()) + 1);
        videoRepository.save(video);

        if (!video.getUser().getId().equals(currentUser.getId())) {
            notificationService.createNotification(
                    video.getUser(),
                    currentUser,
                    video,
                    "LIKE",
                    currentUser.getUsername() + " liked your video: " + video.getTitle()
            );
        }

        videoRepostRepository.findByVideoIdWithUser(video.getId()).stream()
                .map(VideoRepost::getUser)
                .filter(repostUser -> !repostUser.getId().equals(video.getUser().getId()))
                .filter(repostUser -> !repostUser.getId().equals(currentUser.getId()))
                .forEach(repostUser -> notificationService.createNotification(
                        repostUser,
                        currentUser,
                        video,
                        "REPOST_LIKE",
                        currentUser.getUsername() + " liked a video you reposted: " + video.getTitle()
                ));

        return mapToStatsDTO(video, true);
    }

    @Override
    public VideoStatsResponseDTO unlikeVideo(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());

        videoLikeRepository.findByUserIdAndVideoId(currentUser.getId(), id).ifPresent(like -> {
            videoLikeRepository.delete(like);
            video.setLikeCount(Math.max(0L, safe(video.getLikeCount()) - 1));
            videoRepository.save(video);
        });

        return mapToStatsDTO(video, false);
    }

    @Override
    public VideoStatsResponseDTO repostVideo(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        if (!videoRepostRepository.existsByUserIdAndVideoId(currentUser.getId(), id)) {
            videoRepostRepository.save(VideoRepost.builder()
                    .user(currentUser)
                    .video(video)
                    .build());

            notificationService.createNotification(
                    video.getUser(),
                    currentUser,
                    video,
                    "REPOST",
                    currentUser.getUsername() + " reposted your video: " + video.getTitle()
            );
        }

        return mapToStatsDTO(video, videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id));
    }

    @Override
    public VideoStatsResponseDTO unrepostVideo(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        videoRepostRepository.findByUserIdAndVideoId(currentUser.getId(), id)
                .ifPresent(videoRepostRepository::delete);

        return mapToStatsDTO(video, videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id));
    }

    @Override
    public VideoResponseDTO getVideoByUsernameAndId(String username, Long videoId) {
        Video video = videoRepository.findByUserUsernameAndId(username, videoId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        User currentUser = getCurrentUser();
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        return mapToResponseDTO(video);
    }

    @Override
    @Transactional
    public VideoStatsResponseDTO recordVideoView(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        recordView(video, currentUser);
        return mapToStatsDTO(video, currentUser != null && videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id));
    }

    @Override
    public List<VideoDailyMetricResponseDTO> getStudioDailyViews(int days) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        int safeDays = Math.max(1, Math.min(days, 60));
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        Map<LocalDate, Long> countsByDate = videoViewRepository
                .findByOwnerIdAndCreatedAtBetween(currentUser.getId(), start, end)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        view -> view.getCreatedAt().toLocalDate(),
                        java.util.stream.Collectors.counting()
                ));

        List<VideoDailyMetricResponseDTO> result = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            result.add(VideoDailyMetricResponseDTO.builder()
                    .date(date)
                    .views(countsByDate.getOrDefault(date, 0L))
                    .build());
        }

        return result;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void hardDeleteExpiredVideos() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Video> expired = videoRepository.findExpiredVideos(cutoff);

        for (Video video : expired) {
            try {
                videoDeletionService.hardDelete(video);
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

    private Sound resolveSelectedSound(Long soundId) {
        if (soundId == null) return null;

        Sound sound = soundRepository.findByIdAndIsDeletedFalse(soundId)
                .orElseThrow(() -> new AppException(ErrorCode.SOUND_NOT_FOUND));

        if (!Boolean.TRUE.equals(sound.getIsActive()) || !Boolean.TRUE.equals(sound.getIsPublic())) {
            throw new AppException(ErrorCode.SOUND_NOT_FOUND);
        }

        return sound;
    }

    private Sound createOriginalSound(Video video, User user, MultipartFile file, String videoUrl, Integer duration, VideoUploadRequestDTO requestDTO) {
        String audioUrl = audioProcessingService.extractAudioUrl(file, videoUrl);
        String coverUrl = Boolean.TRUE.equals(requestDTO.getUseAvatarAsSoundCover()) && user.getAvatarUrl() != null
                ? user.getAvatarUrl()
                : video.getThumbnailUrl();
        Sound sound = Sound.builder()
                .title("original sound - @" + user.getUsername())
                .artistName(user.getUsername())
                .description(video.getDescription())
                .type(SoundType.ORIGINAL)
                .audioUrl(audioUrl)
                .coverUrl(coverUrl)
                .durationSeconds(duration == null ? 0 : duration)
                .owner(user)
                .sourceVideo(video)
                .usageCount(1L)
                .isPublic(true)
                .isActive(true)
                .isDeleted(false)
                .build();

        return soundRepository.save(sound);
    }

    private VideoResponseDTO mapToResponseDTO(Video video) {
        User currentUser = getCurrentUser();
        if (video.isDeleted()) {
            return VideoResponseDTO.builder()
                    .id(video.getId())
                    .title("Video không khả dụng")
                    .description(null)
                    .fileUrl("")
                    .thumbnailUrl(null)
                    .duration(null)
                    .category(video.getCategory())
                    .viewCount(0L)
                    .likeCount(0L)
                    .commentCount(0L)
                    .saveCount(0L)
                    .shareCount(0L)
                    .userId(video.getUser().getId())
                    .username(video.getUser().getUsername())
                    .userNickname(video.getUser().getNickname())
                    .userAvatarUrl(video.getUser().getAvatarUrl())
                    .createdAt(video.getCreatedAt())
                    .isSaved(false)
                    .isLiked(false)
                    .isReposted(false)
                    .repostedBy(List.of())
                    .isFollowingAuthor(false)
                    .allowComments(false)
                    .visibility(video.getVisibility() != null ? video.getVisibility().name() : "PUBLIC")
                    .deleted(true)
                    .unavailable(true)
                    .sound(soundMapper.toResponseDTO(video.getSound()))
                    .moderationStatus(video.getModerationStatus() != null ? video.getModerationStatus().name() : null)
                    .moderationCheckedAt(video.getModerationCheckedAt())
                    .moderationReasonCode(video.getModerationReasonCode())
                    .moderationReasonMessage(video.getModerationReasonMessage())
                    .musicCopyrightStatus(video.getMusicCopyrightStatus() != null ? video.getMusicCopyrightStatus().name() : null)
                    .musicCopyrightCheckedAt(video.getMusicCopyrightCheckedAt())
                    .musicCopyrightReasonCode(video.getMusicCopyrightReasonCode())
                    .musicCopyrightReasonMessage(video.getMusicCopyrightReasonMessage())
                    .build();
        }
        boolean isSaved = currentUser != null && ISavedVideoRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());
        boolean isLiked = currentUser != null && videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());
        boolean isReposted = currentUser != null && videoRepostRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());
        boolean isFollowingAuthor = currentUser != null
                && !currentUser.getId().equals(video.getUser().getId())
                && followRepo.existsByFollowerAndFollowing(currentUser, video.getUser());
        long shareCount = getShareCount(video.getId());

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
                .saveCount(video.getSaveCount() == null ? 0L : video.getSaveCount())
                .shareCount(shareCount)
                .userId(video.getUser().getId())
                .username(video.getUser().getUsername())
                .userNickname(video.getUser().getNickname())
                .userAvatarUrl(video.getUser().getAvatarUrl())
                .createdAt(video.getCreatedAt())
                .isSaved(isSaved)
                .isLiked(isLiked)
                .isReposted(isReposted)
                .repostedBy(mapRepostUsers(video, currentUser))
                .isFollowingAuthor(isFollowingAuthor)
                .allowComments(video.getAllowComments())
                .visibility(video.getVisibility() != null ? video.getVisibility().name() : "PUBLIC")
                .deleted(false)
                .unavailable(false)
                .sound(soundMapper.toResponseDTO(video.getSound()))
                .moderationStatus(video.getModerationStatus() != null ? video.getModerationStatus().name() : null)
                .moderationCheckedAt(video.getModerationCheckedAt())
                .moderationReasonCode(video.getModerationReasonCode())
                .moderationReasonMessage(video.getModerationReasonMessage())
                .musicCopyrightStatus(video.getMusicCopyrightStatus() != null ? video.getMusicCopyrightStatus().name() : null)
                .musicCopyrightCheckedAt(video.getMusicCopyrightCheckedAt())
                .musicCopyrightReasonCode(video.getMusicCopyrightReasonCode())
                .musicCopyrightReasonMessage(video.getMusicCopyrightReasonMessage())
                .build();
    }

    private VideoStatsResponseDTO mapToStatsDTO(Video video, boolean liked) {
        User currentUser = getCurrentUser();
        boolean isReposted = currentUser != null && videoRepostRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());
        return VideoStatsResponseDTO.builder()
                .videoId(video.getId())
                .liked(liked)
                .viewCount(safe(video.getViewCount()))
                .likeCount(safe(video.getLikeCount()))
                .commentCount(safe(video.getCommentCount()))
                .saveCount(safe(video.getSaveCount()))
                .shareCount(getShareCount(video.getId()))
                .reposted(isReposted)
                .repostedBy(mapRepostUsers(video, currentUser))
                .build();
    }

    private List<VideoRepostUserResponseDTO> mapRepostUsers(Video video, User currentUser) {
        if (currentUser == null) {
            return List.of();
        }

        return videoRepostRepository.findRecentByVideoId(video.getId(), PageRequest.of(0, 20)).stream()
                .filter(repost -> shouldShowRepostUser(repost.getUser(), currentUser))
                .limit(2)
                .map(repost -> mapRepostUser(repost.getUser(), currentUser))
                .toList();
    }

    private long getShareCount(Long videoId) {
        return videoRepostRepository.countByVideoId(videoId)
                + messageAttachmentRepository.countByTypeAndVideoId("VIDEO_POST", videoId);
    }

    private void recordView(Video video, User viewer) {
        videoViewRepository.save(VideoView.builder()
                .video(video)
                .owner(video.getUser())
                .viewer(viewer)
                .build());

        video.setViewCount(safe(video.getViewCount()) + 1);
        videoRepository.save(video);
    }

    private boolean shouldShowRepostUser(User repostUser, User currentUser) {
        if (repostUser.getId().equals(currentUser.getId())) {
            return true;
        }
        return followRepo.existsByFollowerAndFollowing(currentUser, repostUser);
    }

    private VideoRepostUserResponseDTO mapRepostUser(User user, User currentUser) {
        return VideoRepostUserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isCurrentUser(currentUser != null && currentUser.getId().equals(user.getId()))
                .build();
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    @Override
    public VideoResponseDTO updateVideo(Long id, VideoUploadRequestDTO requestDTO) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        if (currentUser == null || !video.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (requestDTO.getTitle() != null) {
            video.setTitle(requestDTO.getTitle());
        }
        if (requestDTO.getDescription() != null) {
            video.setDescription(requestDTO.getDescription());
        }
        if (requestDTO.getCategory() != null) {
            video.setCategory(requestDTO.getCategory());
        }
        if (requestDTO.getVisibility() != null) {
            video.setVisibility(VideoVisibility.valueOf(requestDTO.getVisibility()));
        }
        if (requestDTO.getAllowComments() != null) {
            video.setAllowComments(requestDTO.getAllowComments());
        }
        if (requestDTO.getAllowEdit() != null) {
            video.setAllowEdit(requestDTO.getAllowEdit());
        }

        video = videoRepository.save(video);
        return mapToResponseDTO(video);
    }

    @Override
    public Page<VideoResponseDTO> getLikedVideos(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return videoRepository.findLikedVideosByUserId(currentUser.getId(), pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public Page<VideoResponseDTO> getRepostedVideosByUsername(String username, Pageable pageable) {
        userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User currentUser = getCurrentUser();
        Long viewerId = currentUser == null ? null : currentUser.getId();
        return videoRepostRepository.findRepostedVideosByUsernameVisibleForViewer(username, viewerId, pageable)
                .map(this::mapToResponseDTO);
    }

    private void checkVisibilityOrThrow(Video video, User viewer) {
        if (viewer != null && video.getUser().getId().equals(viewer.getId())) {
            return;
        }

        if (!isPublishedAfterModeration(video)) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }

        if (video.getVisibility() == null || video.getVisibility() == VideoVisibility.PUBLIC) {
            return;
        }

        if (viewer == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (video.getVisibility() == VideoVisibility.PRIVATE) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        if (video.getVisibility() == com.back.video.model.enums.VideoVisibility.FRIENDS) {
            boolean viewerFollowsOwner = followRepo.existsByFollowerAndFollowing(viewer, video.getUser());
            boolean ownerFollowsViewer = followRepo.existsByFollowerAndFollowing(video.getUser(), viewer);
            if (!viewerFollowsOwner || !ownerFollowsViewer) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
        }
    }

    private boolean isPublishedAfterModeration(Video video) {
        return video.getModerationStatus() == VideoModerationStatus.APPROVED
                && video.getMusicCopyrightStatus() != MusicCopyrightStatus.REJECTED;
    }

    @Override
    public Page<VideoResponseDTO> getFollowingFeed(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        Pageable candidatePageable = org.springframework.data.domain.PageRequest.of(0, 500);
        List<Video> candidates = new java.util.ArrayList<>(videoRepository.findFollowingFeed(currentUser.getId(), candidatePageable).getContent());
        
        candidates.sort((v1, v2) -> Double.compare(calculateFollowingScore(v2, currentUser), calculateFollowingScore(v1, currentUser)));
        
        return paginateList(candidates, pageable).map(this::mapToResponseDTO);
    }

    @Override
    public Page<VideoResponseDTO> getFriendsFeed(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        Pageable candidatePageable = org.springframework.data.domain.PageRequest.of(0, 500);
        List<Video> candidates = new java.util.ArrayList<>(videoRepository.findFriendsFeed(currentUser.getId(), candidatePageable).getContent());
        
        candidates.sort((v1, v2) -> Double.compare(calculateFriendsScore(v2, currentUser), calculateFriendsScore(v1, currentUser)));
        
        return paginateList(candidates, pageable).map(this::mapToResponseDTO);
    }

    private Page<Video> paginateList(List<Video> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<Video> subList = start > list.size() ? new java.util.ArrayList<>() : list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(subList, pageable, list.size());
    }

    private double calculateForYouScore(Video video, User currentUser) {
        double interactionScore = (safe(video.getLikeCount()) * 2.0 + safe(video.getCommentCount()) * 3.0 + safe(video.getSaveCount()) * 4.0) / (safe(video.getViewCount()) + 1.0);
        double trendingScore = Math.log1p(safe(video.getViewCount()) + safe(video.getLikeCount()));
        long hoursSinceCreated = java.time.temporal.ChronoUnit.HOURS.between(video.getCreatedAt() != null ? video.getCreatedAt() : LocalDateTime.now(), LocalDateTime.now());
        double freshnessScore = Math.exp(-hoursSinceCreated / 24.0);
        
        long seed = video.getId() + (currentUser != null ? currentUser.getId() : 0L);
        java.util.Random random = new java.util.Random(seed);
        double explorationBoost = random.nextDouble();
        
        return 0.35 * trendingScore + 0.20 * trendingScore + 0.15 * interactionScore + 0.10 * 1.0 + 0.10 * trendingScore + 0.05 * freshnessScore + 0.05 * explorationBoost;
    }

    private double calculateFollowingScore(Video video, User currentUser) {
        long hoursSinceCreated = java.time.temporal.ChronoUnit.HOURS.between(video.getCreatedAt() != null ? video.getCreatedAt() : LocalDateTime.now(), LocalDateTime.now());
        double freshnessScore = Math.exp(-hoursSinceCreated / 48.0);
        double creatorAffinity = 1.0;
        double videoEngagement = (safe(video.getLikeCount()) * 1.0 + safe(video.getCommentCount()) * 2.0) / (safe(video.getViewCount()) + 1.0);
        double completionPrediction = Math.log1p(safe(video.getViewCount()));
        
        long seed = video.getId() + (currentUser != null ? currentUser.getId() : 0L);
        java.util.Random random = new java.util.Random(seed);
        double randomDiversify = random.nextDouble();
        
        return 0.45 * freshnessScore + 0.25 * creatorAffinity + 0.15 * videoEngagement + 0.10 * completionPrediction + 0.05 * randomDiversify;
    }

    private double calculateFriendsScore(Video video, User currentUser) {
        long hoursSinceCreated = java.time.temporal.ChronoUnit.HOURS.between(video.getCreatedAt() != null ? video.getCreatedAt() : LocalDateTime.now(), LocalDateTime.now());
        double freshnessScore = Math.exp(-hoursSinceCreated / 48.0);
        double relationshipStrength = 1.0;
        double interactionHistory = 1.0;
        double videoEngagement = (safe(video.getLikeCount()) * 1.0 + safe(video.getCommentCount()) * 2.0) / (safe(video.getViewCount()) + 1.0);
        
        long seed = video.getId() + (currentUser != null ? currentUser.getId() : 0L);
        java.util.Random random = new java.util.Random(seed);
        double randomDiversify = random.nextDouble();
        
        return 0.35 * relationshipStrength + 0.30 * freshnessScore + 0.20 * interactionHistory + 0.10 * videoEngagement + 0.05 * randomDiversify;
    }
}
