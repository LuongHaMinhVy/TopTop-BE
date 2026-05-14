package com.back.comment.service;

import com.back.comment.model.dto.request.CommentRequestDTO;
import com.back.comment.model.dto.response.CommentResponseDTO;
import com.back.comment.model.entity.Comment;
import com.back.comment.repo.ICommentRepo;
import com.back.notification.service.INotificationService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    private final ICommentRepo commentRepo;
    private final IVideoRepository videoRepository;
    private final INotificationService notificationService;
    private final IUserRepo userRepo;

    @Override
    @Transactional
    public CommentResponseDTO addComment(Long videoId, CommentRequestDTO requestDTO) {
        User user = getCurrentUser();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Comment.CommentBuilder builder = Comment.builder()
                .user(user)
                .video(video)
                .content(requestDTO.getContent());

        if (requestDTO.getParentId() != null) {
            Comment parent = commentRepo.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
            builder.parent(parent);
        }

        Comment comment = builder.build();
        comment = commentRepo.save(comment);

        video.setCommentCount(video.getCommentCount() + 1);
        videoRepository.save(video);

        notificationService.createNotification(
                video.getUser(),
                user,
                video,
                "COMMENT",
                user.getUsername() + " commented on your video: " + requestDTO.getContent()
        );

        return mapToResponseDTO(comment);
    }

    @Override
    public List<CommentResponseDTO> getCommentsByVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        
        return commentRepo.findByVideoWithUser(video).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        User user = getCurrentUser();
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        if (!comment.getUser().getId().equals(user.getId()) && 
            !comment.getVideo().getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Video video = comment.getVideo();
        video.setCommentCount(video.getCommentCount() - 1);
        videoRepository.save(video);

        commentRepo.delete(comment);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    private CommentResponseDTO mapToResponseDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .videoId(comment.getVideo().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
