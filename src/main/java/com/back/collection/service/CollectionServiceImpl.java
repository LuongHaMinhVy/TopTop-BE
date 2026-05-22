package com.back.collection.service;

import com.back.collection.mapper.CollectionMapper;
import com.back.collection.model.dto.request.CreateCollectionRequestDTO;
import com.back.collection.model.dto.request.UpdateCollectionRequestDTO;
import com.back.collection.model.dto.response.CollectionResponseDTO;
import com.back.collection.model.entity.CollectionVideo;
import com.back.collection.model.entity.SavedVideo;
import com.back.collection.model.entity.VideoCollection;
import com.back.collection.repo.ICollectionVideoRepository;
import com.back.collection.repo.ISavedVideoRepository;
import com.back.collection.repo.IVideoCollectionRepository;
import com.back.block.service.IUserBlockService;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.dto.request.VideoResponseDTO;
import com.back.video.model.dto.response.VideoRepostUserResponseDTO;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoLikeRepository;
import com.back.video.repo.IVideoRepository;
import com.back.video.repo.IVideoRepostRepository;
import com.back.follow.repo.IFollowRepo;
import com.back.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements ICollectionService {

    private final ISavedVideoRepository ISavedVideoRepository;
    private final IVideoCollectionRepository IVideoCollectionRepository;
    private final ICollectionVideoRepository ICollectionVideoRepository;
    private final IVideoRepository videoRepository;
    private final IUserRepo userRepo;
    private final CollectionMapper collectionMapper;
    private final IUserBlockService userBlockService;
    private final IVideoLikeRepository videoLikeRepository;
    private final IVideoRepostRepository videoRepostRepository;
    private final IFollowRepo followRepo;
    private final INotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<VideoResponseDTO> getFavoriteVideos(Pageable pageable) {
        User user = getCurrentUserOrThrow();
        return ISavedVideoRepository.findVisibleByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(savedVideo -> mapToVideoResponseDTO(savedVideo.getVideo(), user, true));
    }

    @Override
    @Transactional
    public VideoResponseDTO saveVideo(Long videoId) {
        User user = getCurrentUserOrThrow();
        Video video = getVideoOrThrow(videoId);
        userBlockService.assertNotBlockedEitherWay(user, video.getUser());

        if (!ISavedVideoRepository.existsByUserIdAndVideoId(user.getId(), videoId)) {
            ISavedVideoRepository.save(SavedVideo.builder()
                    .user(user)
                    .video(video)
                    .build());
            video.setSaveCount(getSaveCount(video) + 1);
            videoRepository.save(video);
            notificationService.createNotification(
                    video.getUser(),
                    user,
                    video,
                    "SAVE",
                    user.getUsername() + " saved your video: " + video.getTitle()
            );
        }

        return mapToVideoResponseDTO(video, user, true);
    }

    @Override
    @Transactional
    public VideoResponseDTO unsaveVideo(Long videoId) {
        User user = getCurrentUserOrThrow();
        Video video = getVideoOrThrow(videoId);
        userBlockService.assertNotBlockedEitherWay(user, video.getUser());

        ISavedVideoRepository.findByUserIdAndVideoId(user.getId(), videoId)
                .ifPresent(savedVideo -> {
                    ICollectionVideoRepository.deleteByVideoIdAndCollectionUserId(videoId, user.getId());
                    ISavedVideoRepository.delete(savedVideo);
                    if (getSaveCount(video) > 0) {
                        video.setSaveCount(getSaveCount(video) - 1);
                        videoRepository.save(video);
                    }
                });

        return mapToVideoResponseDTO(video, user, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponseDTO> getCollections() {
        User user = getCurrentUserOrThrow();
        return IVideoCollectionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(collectionMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponseDTO> getUserCollections(String username) {
        User owner = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        User currentUser = getCurrentUserOrNull();
        assertCanViewOwner(currentUser, owner);

        boolean isOwner = currentUser != null && currentUser.getId().equals(owner.getId());
        List<VideoCollection> collections = isOwner
                ? IVideoCollectionRepository.findByUserIdOrderByCreatedAtDesc(owner.getId())
                : IVideoCollectionRepository.findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(owner.getId());

        return collections.stream()
                .map(collectionMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponseDTO getUserCollection(String username, Long collectionId) {
        VideoCollection collection = getCollectionForProfileOrThrow(username, collectionId);
        return collectionMapper.toResponseDTO(collection);
    }

    @Override
    @Transactional
    public CollectionResponseDTO createCollection(CreateCollectionRequestDTO requestDTO) {
        User user = getCurrentUserOrThrow();
        String name = requestDTO.getName().trim();

        if (IVideoCollectionRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            throw new AppException(ErrorCode.COLLECTION_ALREADY_EXISTS, "name");
        }

        VideoCollection collection = IVideoCollectionRepository.save(VideoCollection.builder()
                .user(user)
                .name(name)
                .description(normalizeDescription(requestDTO.getDescription()))
                .isPublic(Boolean.TRUE.equals(requestDTO.getIsPublic()))
                .build());

        return collectionMapper.toResponseDTO(collection);
    }

    @Override
    @Transactional
    public CollectionResponseDTO updateCollection(Long collectionId, UpdateCollectionRequestDTO requestDTO) {
        User user = getCurrentUserOrThrow();
        VideoCollection collection = getOwnedCollectionOrThrow(collectionId, user.getId());
        String name = requestDTO.getName().trim();

        if (IVideoCollectionRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(user.getId(), name, collectionId)) {
            throw new AppException(ErrorCode.COLLECTION_ALREADY_EXISTS, "name");
        }

        collection.setName(name);
        collection.setDescription(normalizeDescription(requestDTO.getDescription()));
        if (requestDTO.getIsPublic() != null) {
            collection.setIsPublic(requestDTO.getIsPublic());
        }

        return collectionMapper.toResponseDTO(IVideoCollectionRepository.save(collection));
    }

    @Override
    @Transactional
    public void deleteCollection(Long collectionId) {
        User user = getCurrentUserOrThrow();
        VideoCollection collection = getOwnedCollectionOrThrow(collectionId, user.getId());

        ICollectionVideoRepository.deleteByCollectionId(collection.getId());
        IVideoCollectionRepository.delete(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoResponseDTO> getCollectionVideos(Long collectionId, Pageable pageable) {
        User user = getCurrentUserOrThrow();
        VideoCollection collection = getOwnedCollectionOrThrow(collectionId, user.getId());

        return ICollectionVideoRepository.findVisibleByCollectionIdOrderByAddedAtDesc(collection.getId(), user.getId(), pageable)
                .map(collectionVideo -> mapToVideoResponseDTO(collectionVideo.getVideo(), user, true));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VideoResponseDTO> getUserCollectionVideos(String username, Long collectionId, Pageable pageable) {
        VideoCollection collection = getCollectionForProfileOrThrow(username, collectionId);
        User currentUser = getCurrentUserOrNull();
        return ICollectionVideoRepository.findVisibleByCollectionIdForViewerOrderByAddedAtDesc(
                        collection.getId(),
                        collection.getUser().getId(),
                        currentUser == null ? null : currentUser.getId(),
                        pageable)
                .map(collectionVideo -> mapToVideoResponseDTO(
                        collectionVideo.getVideo(),
                        currentUser,
                        currentUser != null && ISavedVideoRepository.existsByUserIdAndVideoId(
                                currentUser.getId(),
                                collectionVideo.getVideo().getId())));
    }

    @Override
    @Transactional
    public VideoResponseDTO addVideoToCollection(Long collectionId, Long videoId) {
        User user = getCurrentUserOrThrow();
        VideoCollection collection = getOwnedCollectionOrThrow(collectionId, user.getId());
        Video video = getVideoOrThrow(videoId);
        userBlockService.assertNotBlockedEitherWay(user, video.getUser());

        saveVideo(videoId);

        if (!ICollectionVideoRepository.existsByCollectionIdAndVideoId(collection.getId(), videoId)) {
            ICollectionVideoRepository.save(CollectionVideo.builder()
                    .collection(collection)
                    .video(video)
                    .build());
        }

        return mapToVideoResponseDTO(video, user, true);
    }

    @Override
    @Transactional
    public void removeVideoFromCollection(Long collectionId, Long videoId) {
        User user = getCurrentUserOrThrow();
        VideoCollection collection = getOwnedCollectionOrThrow(collectionId, user.getId());

        ICollectionVideoRepository.findByCollectionIdAndVideoId(collection.getId(), videoId)
                .ifPresent(ICollectionVideoRepository::delete);
    }

    private Video getVideoOrThrow(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        if (video.isDeleted()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        return video;
    }

    private VideoCollection getOwnedCollectionOrThrow(Long collectionId, Long userId) {
        return IVideoCollectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));
    }

    private VideoCollection getCollectionForProfileOrThrow(String username, Long collectionId) {
        User publicOwner = userRepo.findPublicUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        VideoCollection collection = IVideoCollectionRepository.findByIdAndUserUsernameIgnoreCase(collectionId, username)
                .orElseThrow(() -> new AppException(ErrorCode.COLLECTION_NOT_FOUND));

        User currentUser = getCurrentUserOrNull();
        User owner = collection.getUser();
        if (!owner.getId().equals(publicOwner.getId())) {
            throw new AppException(ErrorCode.COLLECTION_NOT_FOUND);
        }
        assertCanViewOwner(currentUser, owner);

        boolean isOwner = currentUser != null && currentUser.getId().equals(owner.getId());
        if (!isOwner && !Boolean.TRUE.equals(collection.getIsPublic())) {
            throw new AppException(ErrorCode.COLLECTION_ACCESS_DENIED);
        }

        return collection;
    }

    private void assertCanViewOwner(User currentUser, User owner) {
        if (currentUser != null) {
            userBlockService.assertNotBlockedEitherWay(currentUser, owner);
        }
    }

    private User getCurrentUserOrNull() {
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

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
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

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private VideoResponseDTO mapToVideoResponseDTO(Video video, User currentUser, boolean isSaved) {
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
                    .allowComments(false)
                    .visibility(video.getVisibility() != null ? video.getVisibility().name() : "PUBLIC")
                    .deleted(true)
                    .unavailable(true)
                    .build();
        }

        boolean isLiked = currentUser != null && videoLikeRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());
        boolean isReposted = currentUser != null && videoRepostRepository.existsByUserIdAndVideoId(currentUser.getId(), video.getId());

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
                .saveCount(getSaveCount(video))
                .shareCount(videoRepostRepository.countByVideoId(video.getId()))
                .userId(video.getUser().getId())
                .username(video.getUser().getUsername())
                .userNickname(video.getUser().getNickname())
                .userAvatarUrl(video.getUser().getAvatarUrl())
                .createdAt(video.getCreatedAt())
                .isSaved(isSaved)
                .isLiked(isLiked)
                .isReposted(isReposted)
                .repostedBy(mapRepostUsers(video, currentUser))
                .allowComments(video.getAllowComments())
                .visibility(video.getVisibility() != null ? video.getVisibility().name() : "PUBLIC")
                .deleted(false)
                .unavailable(false)
                .build();
    }

    private Long getSaveCount(Video video) {
        return video.getSaveCount() == null ? 0L : video.getSaveCount();
    }

    private List<VideoRepostUserResponseDTO> mapRepostUsers(Video video, User currentUser) {
        if (currentUser == null) {
            return List.of();
        }

        return videoRepostRepository.findRecentByVideoId(video.getId(), PageRequest.of(0, 20)).stream()
                .filter(repost -> shouldShowRepostUser(repost.getUser(), currentUser))
                .limit(2)
                .map(repost -> {
                    User user = repost.getUser();
                    return VideoRepostUserResponseDTO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .avatarUrl(user.getAvatarUrl())
                            .isCurrentUser(currentUser != null && currentUser.getId().equals(user.getId()))
                            .build();
                })
                .toList();
    }

    private boolean shouldShowRepostUser(User repostUser, User currentUser) {
        if (repostUser.getId().equals(currentUser.getId())) {
            return true;
        }
        return followRepo.existsByFollowerAndFollowing(currentUser, repostUser);
    }
}
