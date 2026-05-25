package com.back.video.service;

import com.back.chat.repo.IMessageAttachmentRepository;
import com.back.collection.repo.ICollectionVideoRepository;
import com.back.collection.repo.ISavedVideoRepository;
import com.back.common.service.R2StorageService;
import com.back.moderation.repository.IVideoModerationFrameRepository;
import com.back.moderation.repository.IVideoModerationResultRepository;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoLikeRepository;
import com.back.video.repo.IVideoRepository;
import com.back.video.repo.IVideoRepostRepository;
import com.back.video.repo.IVideoViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoDeletionService {

    private final R2StorageService storageService;
    private final IVideoRepository videoRepository;
    private final ICollectionVideoRepository collectionVideoRepository;
    private final ISavedVideoRepository savedVideoRepository;
    private final IVideoRepostRepository videoRepostRepository;
    private final IVideoLikeRepository videoLikeRepository;
    private final IVideoViewRepository videoViewRepository;
    private final IMessageAttachmentRepository messageAttachmentRepository;
    private final IVideoModerationFrameRepository moderationFrameRepository;
    private final IVideoModerationResultRepository moderationResultRepository;

    @Transactional
    public void hardDelete(Video video) {
        Long videoId = video.getId();

        deleteCloudObject(video.getFileUrl(), "video file", videoId);
        deleteCloudObject(video.getThumbnailUrl(), "video cover", videoId);

        messageAttachmentRepository.deleteByVideoId(videoId);
        collectionVideoRepository.deleteByVideoId(videoId);
        savedVideoRepository.deleteByVideoId(videoId);
        videoRepostRepository.deleteByVideoId(videoId);
        videoLikeRepository.deleteByVideoId(videoId);
        videoViewRepository.deleteByVideoId(videoId);
        moderationFrameRepository.deleteByVideoId(videoId);
        moderationResultRepository.deleteByVideoId(videoId);

        videoRepository.delete(video);
        log.info("Hard deleted video id={}", videoId);
    }

    private void deleteCloudObject(String url, String label, Long videoId) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            String key = storageService.extractKeyFromUrl(url);
            storageService.deleteFile(key);
        } catch (Exception e) {
            log.warn("Failed to delete {} for video id={}: {}", label, videoId, e.getMessage());
        }
    }
}
