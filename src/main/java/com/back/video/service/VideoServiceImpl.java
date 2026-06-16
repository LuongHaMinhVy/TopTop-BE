package com.back.video.service;

import com.back.collection.repo.ISavedVideoRepository;
import com.back.collection.repo.ICollectionVideoRepository;
import com.back.chat.repo.IMessageAttachmentRepository;
import com.back.notification.service.INotificationService;

import com.back.block.service.IUserBlockService;
import com.back.common.service.R2StorageService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.recommendation.service.IVideoRecommendationService;
import com.back.sound.mapper.SoundMapper;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.sound.repo.ISoundRepository;
import com.back.sound.service.IAudioProcessingService;
import com.back.user.model.entity.User;
import com.back.user.model.entity.UserContentFilterTag;
import com.back.user.repo.IUserContentFilterTagRepo;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoDailyMetricResponseDTO;
import com.back.video.model.dto.response.VideoDescriptionTranslationResponseDTO;
import com.back.video.model.dto.response.VideoRepostUserResponseDTO;
import com.back.video.model.dto.response.VideoStatsResponseDTO;
import com.back.video.model.dto.response.VideoUploadRequestDTO;
import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoCategory;
import com.back.video.model.entity.VideoLike;
import com.back.video.model.entity.VideoNotInterested;
import com.back.video.model.entity.VideoRepost;
import com.back.video.model.entity.VideoView;
import com.back.video.repo.IVideoLikeRepository;
import com.back.video.repo.IVideoNotInterestedRepository;
import com.back.video.repo.IVideoRepository;
import com.back.video.repo.IVideoRepostRepository;
import com.back.video.repo.IVideoViewRepository;
import com.back.video.repo.IVideoCategoryRepository;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import com.back.video.model.dto.request.InitVideoUploadRequestDTO;
import com.back.video.model.dto.request.CompleteVideoUploadRequestDTO;
import com.back.video.model.dto.response.InitVideoUploadResponseDTO;

import com.back.moderation.service.ITextContentModerationService;
import com.back.moderation.service.IVideoModerationService;

import com.back.recommendation.service.UserInterestProfileService;
import com.back.recommendation.service.UserInterestProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {

    private static final int RECENT_VIEWED_EXCLUSION_LIMIT = 1000;
    private static final java.time.Duration FEED_ORDER_TTL = java.time.Duration.ofMinutes(15);

    private final IVideoRepository videoRepository;
    private final IVideoCategoryRepository videoCategoryRepository;
    private final IVideoLikeRepository videoLikeRepository;
    private final IVideoNotInterestedRepository videoNotInterestedRepository;
    private final IVideoRepostRepository videoRepostRepository;
    private final IUserRepo userRepo;
    private final R2StorageService storageService;
    private final IHashtagRepository hashtagRepo;
    private final ISavedVideoRepository ISavedVideoRepository;
    private final IUserBlockService userBlockService;
    private final com.back.follow.repo.IFollowRepo followRepo;
    private final ISoundRepository soundRepository;
    private final SoundMapper soundMapper;
    private final IAudioProcessingService audioProcessingService;
    private final IMessageAttachmentRepository messageAttachmentRepository;
    private final IVideoViewRepository videoViewRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VideoDeletionService videoDeletionService;
    private final ITextContentModerationService textContentModerationService;
    private final IVideoRecommendationService videoRecommendationService;
    private final IVideoModerationService videoModerationService;
    private final IUserContentFilterTagRepo contentFilterTagRepo;
    private final GeminiDescriptionTranslationService descriptionTranslationService;
    private final UserInterestProfileService userInterestProfileService;
    private final ObjectMapper objectMapper;

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

        boolean contentModerationCheckEnabled = !Boolean.FALSE.equals(requestDTO.getEnableContentModerationCheck());
        boolean musicCopyrightCheckEnabled = !Boolean.FALSE.equals(requestDTO.getEnableMusicCopyrightCheck());

        if (contentModerationCheckEnabled) {
            textContentModerationService.assertAllowed("VIDEO_TEXT", requestDTO.getTitle(), user.getId(), "title");
            if (requestDTO.getDescription() != null) {
                textContentModerationService.assertAllowed("VIDEO_TEXT", requestDTO.getDescription(), user.getId(), "description");
            }
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

        // Serialize edit instructions to JSON for persistence
        String editInstructionsJson = null;
        if (requestDTO.getEditInstructions() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                editInstructionsJson = objectMapper.writeValueAsString(requestDTO.getEditInstructions());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.warn("Failed to serialize edit instructions", e);
            }
        }

        VideoCategory videoCategory = null;
        if (requestDTO.getCategory() != null) {
            videoCategory = videoCategoryRepository.findByCodeIgnoreCaseOrNameIgnoreCase(requestDTO.getCategory(), requestDTO.getCategory())
                    .orElseGet(() -> videoCategoryRepository.findByCodeIgnoreCase("OTHER").orElse(null));
        }

        Video video = Video.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .category(videoCategory != null ? videoCategory.getName() : requestDTO.getCategory())
                .videoCategory(videoCategory)
                .fileUrl(fileUrl)
                .thumbnailUrl(coverUrl)
                .duration(duration)
                .user(user)
                .hashtags(extractedHashtags)
                .visibility(requestDTO.getVisibility() != null ? VideoVisibility.valueOf(requestDTO.getVisibility()) : VideoVisibility.PUBLIC)
                .allowComments(requestDTO.getAllowComments() != null ? requestDTO.getAllowComments() : true)
                .allowEdit(requestDTO.getAllowEdit() != null ? requestDTO.getAllowEdit() : false)
                .sound(selectedSound)
                .editInstructionsJson(editInstructionsJson)
                .build();

        video = videoRepository.save(video);

        if (selectedSound != null) {
            soundRepository.incrementUsageCount(selectedSound.getId());
        }

        redisTemplate.delete("upload:" + requestDTO.getUploadId());

        // Trigger async moderation
        final Long videoId = video.getId();
        videoModerationService.runModeration(videoId, musicCopyrightCheckEnabled, contentModerationCheckEnabled);

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

        boolean contentModerationCheckEnabled = !Boolean.FALSE.equals(requestDTO.getEnableContentModerationCheck());
        boolean musicCopyrightCheckEnabled = !Boolean.FALSE.equals(requestDTO.getEnableMusicCopyrightCheck());

        if (contentModerationCheckEnabled) {
            textContentModerationService.assertAllowed("VIDEO_TEXT", requestDTO.getTitle(), user.getId(), "title");
            if (requestDTO.getDescription() != null) {
                textContentModerationService.assertAllowed("VIDEO_TEXT", requestDTO.getDescription(), user.getId(), "description");
            }
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

        VideoCategory videoCategory = null;
        if (requestDTO.getCategory() != null) {
            videoCategory = videoCategoryRepository.findByCodeIgnoreCaseOrNameIgnoreCase(requestDTO.getCategory(), requestDTO.getCategory())
                    .orElseGet(() -> videoCategoryRepository.findByCodeIgnoreCase("OTHER").orElse(null));
        }

        Video video = Video.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .category(videoCategory != null ? videoCategory.getName() : requestDTO.getCategory())
                .videoCategory(videoCategory)
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

        videoModerationService.runModeration(video.getId(), musicCopyrightCheckEnabled, contentModerationCheckEnabled);

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
    public VideoDescriptionTranslationResponseDTO translateDescription(Long id, String targetLocale) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        String sourceText = firstNonBlank(video.getDescription(), video.getTitle());
        String normalizedLocale = normalizeTranslationLocale(targetLocale);
        String translatedText = descriptionTranslationService.translate(sourceText, normalizedLocale);

        return VideoDescriptionTranslationResponseDTO.builder()
                .videoId(video.getId())
                .sourceText(sourceText)
                .translatedText(translatedText)
                .targetLocale(normalizedLocale)
                .build();
    }

    @Override
    public Page<VideoResponseDTO> getAllVideos(Pageable pageable) {
        User currentUser = getCurrentUser();
        Long viewerId = currentUser == null ? null : currentUser.getId();

        // Dynamically scale candidate limits based on total approved video count
        long totalVideos = videoRepository.countByModerationStatus(VideoModerationStatus.APPROVED);
        if (totalVideos == 0) {
            totalVideos = videoRepository.count();
        }

        int newestLimit = (int)Math.clamp(totalVideos * 0.8, 50, 400);
        int viralLimit = (int)Math.clamp(totalVideos * 0.4, 20, 150);
        int followingLimit = (int)Math.clamp(totalVideos * 0.2, 10, 100);
        int discoveryLimit = (int)Math.clamp(totalVideos * 0.3, 10, 150);
        int exclusionLimit = Math.clamp((int)(totalVideos * 0.9), 50, RECENT_VIEWED_EXCLUSION_LIMIT);

        // 1. Newest
        List<Video> newest = videoRepository.findAllVisibleForViewer(viewerId, PageRequest.of(0, newestLimit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        Set<Video> candidateSet = new LinkedHashSet<>(newest);
        
        // 2. Viral - high interaction in the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Video> viral = videoRepository.findViralVideos(viewerId, sevenDaysAgo, PageRequest.of(0, viralLimit));
        candidateSet.addAll(viral);
        
        // 3. Creator - from followed creators
        if (viewerId != null) {
            try {
                List<Video> creator = videoRepository.findFollowingFeed(viewerId, PageRequest.of(0, followingLimit)).getContent();
                candidateSet.addAll(creator);
            } catch (Exception e) {
                log.warn("Failed to retrieve creator pool: {}", e.getMessage());
            }
        }
        
        // 4. Discovery - random videos
        try {
            int randomPage = totalVideos > discoveryLimit ? new Random().nextInt((int) (totalVideos / discoveryLimit)) : 0;
            List<Video> discovery = videoRepository.findAllVisibleForViewer(viewerId, PageRequest.of(randomPage, discoveryLimit)).getContent();
            candidateSet.addAll(discovery);
        } catch (Exception e) {
            log.warn("Failed to retrieve discovery pool: {}", e.getMessage());
        }

        List<Video> candidates = new ArrayList<>(candidateSet);
        candidates = distinctVideos(applyViewerFilters(candidates, currentUser));

        // Filtering out avoidCategories
        UserInterestProfile profile = currentUser != null ? userInterestProfileService.getUserProfile(currentUser.getId()) : null;
        if (profile != null && profile.getAvoidCategories() != null && !profile.getAvoidCategories().isEmpty()) {
            candidates.removeIf(video -> {
                String videoCategory = video.getAiCategory() != null ? video.getAiCategory() : video.getCategory();
                return videoCategory != null && profile.getAvoidCategories().contains(videoCategory);
            });
        }

        if (currentUser != null && pageable.getPageNumber() > 0) {
            List<Long> cachedOrder = readFeedOrder(currentUser.getId());
            if (!cachedOrder.isEmpty()) {
                candidates = distinctVideos(applyViewerFilters(applyFeedOrder(candidates, cachedOrder), currentUser));
                return paginateList(candidates, pageable).map(this::mapToResponseDTO);
            }
        }

        List<Long> recentlyViewedIds = currentUser == null
                ? List.of()
                : videoViewRepository.findRecentViewedVideoIdsByViewerId(
                        currentUser.getId(),
                        PageRequest.of(0, exclusionLimit));

        List<Video> unseenCandidates = excludeVideoIds(candidates, recentlyViewedIds);
        List<Video> rankedCandidates = unseenCandidates.isEmpty() ? candidates : unseenCandidates;

        // Bulk fetch average completion rates for candidates
        List<Long> rankedIdsForRates = rankedCandidates.stream().map(Video::getId).collect(Collectors.toList());
        Map<Long, Double> avgCompletionRates = getAverageCompletionRates(rankedIdsForRates);

        // Generate stable random values for the current request's candidates to enable dynamic shuffling on refresh
        Map<Long, Double> requestRandomValues = new HashMap<>();
        Random threadRandom = ThreadLocalRandom.current();
        for (Video v : rankedCandidates) {
            requestRandomValues.put(v.getId(), threadRandom.nextDouble());
        }

        // Sort candidates
        UserInterestProfile finalProfile = profile;
        rankedCandidates.sort((v1, v2) -> Double.compare(
                calculateForYouScore(v2, currentUser, finalProfile, avgCompletionRates, requestRandomValues),
                calculateForYouScore(v1, currentUser, finalProfile, avgCompletionRates, requestRandomValues)
        ));

        // AI re-ranking if enabled
        rankedCandidates = distinctVideos(new ArrayList<>(videoRecommendationService.rankForYou(rankedCandidates, currentUser)));

        // Apply diversity post-processing (author clustering protection)
        rankedCandidates = applyDiversityPostProcessing(rankedCandidates);

        if (!recentlyViewedIds.isEmpty()) {
            Set<Long> rankedIds = rankedCandidates.stream()
                    .map(Video::getId)
                    .collect(Collectors.toSet());
            candidates.stream()
                    .filter(video -> !rankedIds.contains(video.getId()))
                    .forEach(rankedCandidates::add);
            rankedCandidates = distinctVideos(rankedCandidates);
        }

        if (currentUser != null && pageable.getPageNumber() == 0) {
            cacheFeedOrder(currentUser.getId(), rankedCandidates);
        }
        
        return paginateList(rankedCandidates, pageable).map(this::mapToResponseDTO);
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

        userInterestProfileService.evictProfile(currentUser.getId());

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
    @Transactional
    public void markNotInterested(Long id) {
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

        if (video.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_MARK_OWN_VIDEO_NOT_INTERESTED);
        }

        boolean isLiked = videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id);
        boolean isSaved = ISavedVideoRepository.existsByUserIdAndVideoId(currentUser.getId(), id);
        if (isLiked || isSaved) {
            throw new AppException(ErrorCode.CANNOT_MARK_INTERESTED_VIDEO_NOT_INTERESTED);
        }

        if (!videoNotInterestedRepository.existsByUserIdAndVideoId(currentUser.getId(), id)) {
            videoNotInterestedRepository.save(VideoNotInterested.builder()
                    .user(currentUser)
                    .video(video)
                    .build());
        }

        redisTemplate.delete(feedOrderKey(currentUser.getId()));
        userInterestProfileService.evictProfile(currentUser.getId());
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
            userInterestProfileService.evictProfile(currentUser.getId());
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
        if (video.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_REPOST_SELF);
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
    public VideoStatsResponseDTO recordVideoView(Long id, Long watchDurationMs) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        userBlockService.assertNotBlockedEitherWay(currentUser, video.getUser());
        checkVisibilityOrThrow(video, currentUser);

        recordView(video, currentUser, watchDurationMs);
        return mapToStatsDTO(video, currentUser != null && videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), id));
    }

    @Override
    public List<VideoDailyMetricResponseDTO> getStudioDailyViews(int days) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        int safeDays = Math.clamp(days, 1, 60);
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
                    .videoCategoryId(video.getVideoCategory() != null ? video.getVideoCategory().getId() : null)
                    .videoCategoryCode(video.getVideoCategory() != null ? video.getVideoCategory().getCode() : null)
                    .videoCategoryName(video.getVideoCategory() != null ? video.getVideoCategory().getName() : null)
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
                    .qualityIssuesJson(video.getQualityIssuesJson())
                    .qualityIssueMessage(video.getQualityIssueMessage())
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
                .videoCategoryId(video.getVideoCategory() != null ? video.getVideoCategory().getId() : null)
                .videoCategoryCode(video.getVideoCategory() != null ? video.getVideoCategory().getCode() : null)
                .videoCategoryName(video.getVideoCategory() != null ? video.getVideoCategory().getName() : null)
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
                .qualityIssuesJson(video.getQualityIssuesJson())
                .qualityIssueMessage(video.getQualityIssueMessage())
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

    private void recordView(Video video, User viewer, Long watchDurationMs) {
        Double completionRate = null;
        if (watchDurationMs != null && video.getDuration() != null && video.getDuration() > 0) {
            completionRate = (double) watchDurationMs / (video.getDuration() * 1000.0);
            // Cap at 5.0 to handle looping watch behavior without allowing outliers to excessively skew statistics
            if (completionRate > 5.0) {
                completionRate = 5.0;
            }
        }
        videoViewRepository.save(VideoView.builder()
                .video(video)
                .owner(video.getUser())
                .viewer(viewer)
                .watchDurationMs(watchDurationMs)
                .completionRate(completionRate)
                .build());

        video.setViewCount(safe(video.getViewCount()) + 1);
        videoRepository.save(video);

        if (viewer != null) {
            userInterestProfileService.evictProfile(viewer.getId());
        }
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
            VideoCategory videoCategory = videoCategoryRepository.findByCodeIgnoreCaseOrNameIgnoreCase(requestDTO.getCategory(), requestDTO.getCategory())
                    .orElseGet(() -> videoCategoryRepository.findByCodeIgnoreCase("OTHER").orElse(null));
            video.setVideoCategory(videoCategory);
            video.setCategory(videoCategory != null ? videoCategory.getName() : requestDTO.getCategory());
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
        if (requestDTO.getEditInstructions() != null) {
            Long selectedSoundId = requestDTO.getEditInstructions().getSelectedSoundId();
            Sound previousSound = video.getSound();
            Sound selectedSound = selectedSoundId == null ? null : resolveSelectedSound(selectedSoundId);
            video.setSound(selectedSound);
            if (selectedSound != null && (previousSound == null || !previousSound.getId().equals(selectedSound.getId()))) {
                soundRepository.incrementUsageCount(selectedSound.getId());
            }
        } else if (requestDTO.getSoundId() != null) {
            Sound previousSound = video.getSound();
            Sound selectedSound = resolveSelectedSound(requestDTO.getSoundId());
            video.setSound(selectedSound);
            if (selectedSound != null && (previousSound == null || !previousSound.getId().equals(selectedSound.getId()))) {
                soundRepository.incrementUsageCount(selectedSound.getId());
            }
        }
        if (requestDTO.getEditInstructions() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                video.setEditInstructionsJson(objectMapper.writeValueAsString(requestDTO.getEditInstructions()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.warn("Failed to serialize edit instructions for video {}", id, e);
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
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
    public Page<VideoResponseDTO> getLikedVideosByUsername(String username, Pageable pageable) {
        User targetUser = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User currentUser = getCurrentUser();
        boolean isOwner = currentUser != null && targetUser.getId().equals(currentUser.getId());
        if (!isOwner && !Boolean.TRUE.equals(targetUser.getShowLikedVideos())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        return videoRepository.findLikedVideosByUserId(targetUser.getId(), pageable)
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
        return video.getModerationStatus() == VideoModerationStatus.APPROVED;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private String normalizeTranslationLocale(String targetLocale) {
        if (targetLocale == null || targetLocale.isBlank()) {
            return "vi";
        }
        String normalized = targetLocale.toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("vi")) {
            return "vi";
        }
        if (normalized.startsWith("en")) {
            return "en";
        }
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    @Override
    public Page<VideoResponseDTO> getFollowingFeed(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        Pageable candidatePageable = org.springframework.data.domain.PageRequest.of(0, 500);
        List<Video> candidates = new java.util.ArrayList<>(videoRepository.findFollowingFeed(currentUser.getId(), candidatePageable).getContent());
        candidates = applyViewerFilters(candidates, currentUser);
        
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
        candidates = applyViewerFilters(candidates, currentUser);
        
        candidates.sort((v1, v2) -> Double.compare(calculateFriendsScore(v2, currentUser), calculateFriendsScore(v1, currentUser)));
        
        return paginateList(candidates, pageable).map(this::mapToResponseDTO);
    }

    private List<Video> excludeVideoIds(List<Video> candidates, List<Long> excludedIds) {
        if (excludedIds == null || excludedIds.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        Set<Long> excluded = new HashSet<>(excludedIds);
        return candidates.stream()
                .filter(video -> !excluded.contains(video.getId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<Video> applyContentFilters(List<Video> candidates, User currentUser) {
        if (currentUser == null || candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        Set<String> filteredTags = contentFilterTagRepo.findByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(UserContentFilterTag::getTag)
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());

        if (filteredTags.isEmpty()) {
            return candidates;
        }

        return candidates.stream()
                .filter(video -> video.getHashtags() == null || video.getHashtags().stream()
                        .map(Hashtag::getName)
                        .filter(tag -> tag != null && !tag.isBlank())
                        .map(String::toLowerCase)
                        .noneMatch(filteredTags::contains))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<Video> applyViewerFilters(List<Video> candidates, User currentUser) {
        List<Video> filtered = applyContentFilters(candidates, currentUser);
        if (filtered == null || filtered.isEmpty()) {
            return filtered;
        }

        if (currentUser != null) {
            final Long currentUserId = currentUser.getId();
            filtered = filtered.stream()
                    .filter(video -> video.getUser() == null || !video.getUser().getId().equals(currentUserId))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        if (currentUser == null) {
            return filtered;
        }

        List<Long> notInterestedIds = videoNotInterestedRepository.findVideoIdsByUserId(currentUser.getId());
        return excludeVideoIds(filtered, notInterestedIds);
    }

    private List<Video> distinctVideos(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> seenIds = new LinkedHashSet<>();
        return videos.stream()
                .filter(video -> video != null && video.getId() != null && seenIds.add(video.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void cacheFeedOrder(Long userId, List<Video> videos) {
        List<Long> orderedIds = distinctVideos(videos).stream()
                .map(Video::getId)
                .toList();
        try {
            redisTemplate.opsForValue().set(feedOrderKey(userId), orderedIds, FEED_ORDER_TTL);
        } catch (Exception e) {
            log.warn("Could not cache feed order for user {}: {}", userId, e.getMessage());
        }
    }

    private List<Long> readFeedOrder(Long userId) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(feedOrderKey(userId));
        } catch (Exception e) {
            log.warn("Could not read feed order for user {}: {}", userId, e.getMessage());
            return List.of();
        }

        if (!(cached instanceof List<?> cachedList)) {
            return List.of();
        }

        return cachedList.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }

    private String feedOrderKey(Long userId) {
        return "feed:foryou:order:" + userId;
    }

    private List<Video> applyFeedOrder(List<Video> candidates, List<Long> orderedIds) {
        Map<Long, Video> byId = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(Video::getId, video -> video, (left, right) -> left));

        List<Video> ordered = orderedIds.stream()
                .map(byId::get)
                .filter(video -> video != null)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Set<Long> orderedVideoIds = ordered.stream()
                .map(Video::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        candidates.stream()
                .filter(video -> !orderedVideoIds.contains(video.getId()))
                .forEach(ordered::add);
        return ordered;
    }

    private Page<Video> paginateList(List<Video> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<Video> subList = start > list.size() ? new java.util.ArrayList<>() : list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(subList, pageable, list.size());
    }

    private double calculateForYouScore(Video video, User currentUser, UserInterestProfile profile, Map<Long, Double> avgCompletionRates, Map<Long, Double> requestRandomValues) {
        // 1. Engagement (25%)
        double interactionScore = (safe(video.getLikeCount()) * 2.0 
                                + safe(video.getCommentCount()) * 3.0 
                                + safe(video.getSaveCount()) * 4.0) 
                                / (safe(video.getViewCount()) + 1.0);
        double engagement = Math.min(1.0, interactionScore / 5.0);

        // 2. Trending Velocity (15%)
        double trendingScore = Math.log1p(safe(video.getViewCount()) + safe(video.getLikeCount()));
        double trendingVelocity = Math.min(1.0, trendingScore / 15.0);

        // 3. Freshness Decay (10%)
        long hoursSinceCreated = java.time.temporal.ChronoUnit.HOURS.between(video.getCreatedAt() != null ? video.getCreatedAt() : LocalDateTime.now(java.time.ZoneOffset.UTC), LocalDateTime.now(java.time.ZoneOffset.UTC));
        double freshnessScore = Math.exp(-hoursSinceCreated / 24.0);

        // 4. Completion Rate (20%) - allow looping (completionRate > 1.0) up to a max of 3.0 to boost highly-rewatched content
        double completionRate = avgCompletionRates != null ? avgCompletionRates.getOrDefault(video.getId(), 0.0) : 0.0;
        completionRate = Math.min(3.0, completionRate);

        // 5. Topic Relevance (15%)
        double topicRelevance = 0.0;
        if (profile != null) {
            VideoCategory categoryObj = video.getVideoCategory();
            if (categoryObj != null) {
                if (profile.getAvoidCategoryIds() != null && profile.getAvoidCategoryIds().contains(categoryObj.getId())) {
                    topicRelevance = -10.0; // Heavy penalty
                } else {
                    double categoryRelevance = 0.0;
                    if (profile.getTopCategoryIds() != null && profile.getTopCategoryIds().contains(categoryObj.getId())) {
                        categoryRelevance = 1.0;
                    }
                    
                    double tagRelevance = 0.0;
                    if (profile.getTopHashtags() != null && !profile.getTopHashtags().isEmpty()) {
                        int matchCount = 0;
                        if (video.getAiTagsJson() != null) {
                            try {
                                List<String> aiTags = objectMapper.readValue(video.getAiTagsJson(), new TypeReference<List<String>>() {});
                                for (String tag : aiTags) {
                                    if (profile.getTopHashtags().contains(tag.toLowerCase())) {
                                        matchCount++;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        tagRelevance = (double) matchCount / Math.max(1, profile.getTopHashtags().size());
                    }
                    
                    topicRelevance = 0.7 * categoryRelevance + 0.3 * tagRelevance;
                }
            } else {
                // Fallback to legacy string-based comparison
                String videoCategory = video.getAiCategory() != null ? video.getAiCategory() : video.getCategory();
                if (videoCategory != null && !videoCategory.isBlank()) {
                    if (profile.getAvoidCategories() != null && profile.getAvoidCategories().contains(videoCategory)) {
                        topicRelevance = -10.0;
                    } else {
                        double categoryRelevance = 0.0;
                        if (profile.getTopCategories() != null && profile.getTopCategories().contains(videoCategory)) {
                            categoryRelevance = 1.0;
                        }
                        
                        double tagRelevance = 0.0;
                        if (profile.getTopHashtags() != null && !profile.getTopHashtags().isEmpty()) {
                            int matchCount = 0;
                            if (video.getAiTagsJson() != null) {
                                try {
                                    List<String> aiTags = objectMapper.readValue(video.getAiTagsJson(), new TypeReference<List<String>>() {});
                                    for (String tag : aiTags) {
                                        if (profile.getTopHashtags().contains(tag.toLowerCase())) {
                                            matchCount++;
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                            tagRelevance = (double) matchCount / Math.max(1, profile.getTopHashtags().size());
                        }
                        
                        topicRelevance = 0.7 * categoryRelevance + 0.3 * tagRelevance;
                    }
                }
            }
        }

        // 6. Creator Affinity (10%)
        double creatorAffinity = 0.0;
        if (profile != null && video.getUser() != null) {
            if (profile.getFavoriteCreatorIds() != null && profile.getFavoriteCreatorIds().contains(video.getUser().getId())) {
                creatorAffinity = 1.0;
            }
        }

        // 7. Exploration Boost (5%) - utilizes request-scoped random values to ensure a fresh, shuffled feed on every F5
        double explorationBoost = requestRandomValues != null ? requestRandomValues.getOrDefault(video.getId(), 0.0) : 0.0;

        // Weighted Sum
        return 0.25 * engagement 
             + 0.15 * trendingVelocity 
             + 0.10 * freshnessScore 
             + 0.20 * completionRate 
             + 0.15 * topicRelevance 
             + 0.10 * creatorAffinity 
             + 0.05 * explorationBoost;
    }

    private Map<Long, Double> getAverageCompletionRates(List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) return Map.of();
        try {
            List<Object[]> results = videoViewRepository.getAverageCompletionRates(videoIds);
            Map<Long, Double> map = new java.util.HashMap<>();
            for (Object[] res : results) {
                Long videoId = (Long) res[0];
                Double rate = (Double) res[1];
                map.put(videoId, rate != null ? rate : 0.0);
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to retrieve average completion rates: {}", e.getMessage());
            return Map.of();
        }
    }

    private List<Video> applyDiversityPostProcessing(List<Video> videos) {
        if (videos == null || videos.size() <= 2) {
            return videos;
        }
        
        List<Video> result = new ArrayList<>();
        List<Video> buffer = new ArrayList<>();
        
        for (Video video : videos) {
            if (video.getUser() == null) {
                result.add(video);
                continue;
            }
            
            Long authorId = video.getUser().getId();
            int consecutiveCount = 0;
            for (int i = result.size() - 1; i >= 0; i--) {
                Video prev = result.get(i);
                if (prev.getUser() != null && prev.getUser().getId().equals(authorId)) {
                    consecutiveCount++;
                } else {
                    break;
                }
            }
            
            if (consecutiveCount < 2) {
                result.add(video);
            } else {
                buffer.add(video);
            }
        }
        
        result.addAll(buffer);
        return result;
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

    @Override
    public List<VideoCategory> getActiveCategories() {
        return videoCategoryRepository.findByIsActiveTrue();
    }
}

