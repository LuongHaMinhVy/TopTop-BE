package com.back.comment.service;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentAuthorResponseDTO;
import com.back.comment.model.dto.response.CommentLikeResponseDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import com.back.comment.model.entity.CommentLike;
import com.back.comment.model.enums.CommentStatus;
import com.back.comment.model.entity.Comment;
import com.back.comment.repo.ICommentLikeRepo;
import com.back.comment.repo.ICommentRepo;
import com.back.block.service.IUserBlockService;
import com.back.notification.service.INotificationService;
import com.back.user.model.entity.User;
import com.back.user.model.enums.RoleName;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    private final ICommentRepo commentRepo;
    private final ICommentLikeRepo commentLikeRepo;
    private final IVideoRepository videoRepository;
    private final INotificationService notificationService;
    private final IUserRepo userRepo;
    private final IUserBlockService userBlockService;

    @Override
    @Transactional
    public CommentResponseDTO addComment(Long videoId, CommentRequestDTO requestDTO) {
        User user = getCurrentUser();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        userBlockService.assertNotBlockedEitherWay(user, video.getUser());
        validateCommentAllowed(video);

        Comment.CommentBuilder builder = Comment.builder()
                .user(user)
                .video(video)
                .content(normalizeContent(requestDTO.getContent()))
                .mediaUrl(normalizeMediaUrl(requestDTO.getMediaUrl()))
                .mediaType(normalizeMediaType(requestDTO.getMediaType()))
                .timestampInVideo(requestDTO.getTimestampInVideo())
                .status(CommentStatus.ACTIVE)
                .likeCount(0L)
                .replyCount(0L);

        if (requestDTO.getParentId() != null) {
            Comment parent = findActiveComment(requestDTO.getParentId());
            if (!parent.getVideo().getId().equals(videoId)) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            builder.parent(parent);
            parent.setReplyCount(safe(parent.getReplyCount()) + 1);
            commentRepo.save(parent);
        }

        Comment comment = builder.build();
        comment = commentRepo.save(comment);

        video.setCommentCount(safe(video.getCommentCount()) + 1);
        videoRepository.save(video);

        if (!video.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                    video.getUser(),
                    user,
                    video,
                    "COMMENT",
                    user.getUsername() + " commented on your video: " + comment.getContent()
            );
        }

        return mapToResponseDTO(comment);
    }

    @Override
    public Page<CommentResponseDTO> getCommentsByVideo(Long videoId, Pageable pageable) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
        userBlockService.assertNotBlockedEitherWay(getCurrentUserOrNull(), video.getUser());
        
        return commentRepo.findByVideoWithUser(video, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public Page<CommentResponseDTO> getReplies(Long commentId, Pageable pageable) {
        Comment parent = findActiveComment(commentId);
        userBlockService.assertNotBlockedEitherWay(getCurrentUserOrNull(), parent.getVideo().getUser());

        return commentRepo.findRepliesWithUser(parent, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public CommentResponseDTO addReply(Long commentId, CommentRequestDTO requestDTO) {
        Comment parent = findActiveComment(commentId);
        requestDTO.setParentId(parent.getId());
        return addComment(parent.getVideo().getId(), requestDTO);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        User user = getCurrentUser();
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!canDelete(comment, user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (isDeleted(comment)) {
            return;
        }

        Video video = comment.getVideo();
        video.setCommentCount(Math.max(0L, safe(video.getCommentCount()) - 1));
        videoRepository.save(video);

        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            parent.setReplyCount(Math.max(0L, safe(parent.getReplyCount()) - 1));
            commentRepo.save(parent);
        }

        comment.setStatus(CommentStatus.DELETED);
        comment.setDeletedAt(java.time.LocalDateTime.now());
        comment.setContent("");
        commentRepo.save(comment);
    }

    @Override
    @Transactional
    public CommentLikeResponseDTO likeComment(Long commentId) {
        User user = getCurrentUser();
        Comment comment = findActiveComment(commentId);
        userBlockService.assertNotBlockedEitherWay(user, comment.getVideo().getUser());

        if (!commentLikeRepo.existsByUserIdAndCommentId(user.getId(), commentId)) {
            CommentLike like = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .build();
            commentLikeRepo.save(like);
            comment.setLikeCount(safe(comment.getLikeCount()) + 1);
            commentRepo.save(comment);
        }

        return mapLikeResponse(comment, true);
    }

    @Override
    @Transactional
    public CommentLikeResponseDTO unlikeComment(Long commentId) {
        User user = getCurrentUser();
        Comment comment = findActiveComment(commentId);
        userBlockService.assertNotBlockedEitherWay(user, comment.getVideo().getUser());

        commentLikeRepo.findByUserIdAndCommentId(user.getId(), commentId).ifPresent(like -> {
            commentLikeRepo.delete(like);
            comment.setLikeCount(Math.max(0L, safe(comment.getLikeCount()) - 1));
            commentRepo.save(comment);
        });

        return mapLikeResponse(comment, false);
    }

    private User getCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
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

    private CommentResponseDTO mapToResponseDTO(Comment comment) {
        User currentUser = getCurrentUserOrNull();
        boolean deleted = isDeleted(comment);
        boolean liked = currentUser != null && commentLikeRepo.existsByUserIdAndCommentId(currentUser.getId(), comment.getId());
        boolean canDelete = currentUser != null && canDelete(comment, currentUser);

        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(deleted ? "" : comment.getContent())
                .mediaUrl(deleted ? null : comment.getMediaUrl())
                .mediaType(deleted ? null : comment.getMediaType())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .videoId(comment.getVideo().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .timestampInVideo(comment.getTimestampInVideo())
                .author(CommentAuthorResponseDTO.builder()
                        .id(comment.getUser().getId())
                        .username(comment.getUser().getUsername())
                        .displayName(comment.getUser().getNickname())
                        .avatarUrl(comment.getUser().getAvatarUrl())
                        .verified(comment.getUser().getVerified())
                        .build())
                .likeCount(safe(comment.getLikeCount()))
                .replyCount(safe(comment.getReplyCount()))
                .liked(liked)
                .canDelete(canDelete)
                .deleted(deleted)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private CommentLikeResponseDTO mapLikeResponse(Comment comment, boolean liked) {
        return CommentLikeResponseDTO.builder()
                .commentId(comment.getId())
                .liked(liked)
                .likeCount(safe(comment.getLikeCount()))
                .build();
    }

    private Comment findActiveComment(Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (isDeleted(comment)) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateCommentAllowed(Video video) {
        if (Boolean.FALSE.equals(video.getAllowComments()) || Boolean.FALSE.equals(video.getUser().getAllowComments())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() > 2000) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeMediaUrl(String mediaUrl) {
        String normalized = mediaUrl == null ? "" : mediaUrl.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeMediaType(String mediaType) {
        String normalized = mediaType == null ? "" : mediaType.trim().toUpperCase();
        if (normalized.isBlank()) return null;
        if (!normalized.equals("IMAGE")) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private boolean canDelete(Comment comment, User user) {
        if (comment.getUser().getId().equals(user.getId())) {
            return true;
        }
        if (comment.getVideo().getUser().getId().equals(user.getId())) {
            return true;
        }
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN || role.getName() == RoleName.ROLE_MODERATOR);
    }

    private boolean isDeleted(Comment comment) {
        return comment.getDeletedAt() != null || comment.getStatus() == CommentStatus.DELETED;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}
