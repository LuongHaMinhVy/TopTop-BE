package com.back.recommendation.service;

import com.back.collection.repo.ISavedVideoRepository;
import com.back.collection.model.entity.SavedVideo;
import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoView;
import com.back.video.repo.IVideoLikeRepository;
import com.back.video.repo.IVideoNotInterestedRepository;
import com.back.video.repo.IVideoViewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInterestProfileService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IVideoLikeRepository videoLikeRepository;
    private final IVideoNotInterestedRepository videoNotInterestedRepository;
    private final IVideoViewRepository videoViewRepository;
    private final ISavedVideoRepository savedVideoRepository;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "user:interest:profile:";
    private static final long CACHE_TTL_HOURS = 2;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    private String getCacheKey(Long userId) {
        return CACHE_PREFIX + userId;
    }

    public UserInterestProfile getUserProfile(Long userId) {
        if (userId == null) {
            return new UserInterestProfile();
        }

        String key = getCacheKey(userId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (cached instanceof UserInterestProfile) {
                    return (UserInterestProfile) cached;
                }
                // If it is stored as a linked hash map or string by Redis serializer
                return objectMapper.convertValue(cached, UserInterestProfile.class);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve user interest profile from Redis for user {}: {}", userId, e.getMessage());
        }

        UserInterestProfile profile = buildUserProfile(userId);

        try {
            redisTemplate.opsForValue().set(key, profile, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache user interest profile in Redis for user {}: {}", userId, e.getMessage());
        }

        return profile;
    }

    public void evictProfile(Long userId) {
        if (userId == null) return;
        try {
            redisTemplate.delete(getCacheKey(userId));
            log.info("Evicted user interest profile cache for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to evict user interest profile cache for user {}: {}", userId, e.getMessage());
        }
    }

    private UserInterestProfile buildUserProfile(Long userId) {
        log.info("Building user interest profile from database for user {}", userId);

        // Fetch recent likes (limit to 50)
        List<Video> likedVideos = videoLikeRepository.findRecentLikedVideosByUserId(userId, PageRequest.of(0, 50));

        // Fetch recent saves (limit to 50)
        List<Video> savedVideos = List.of();
        try {
            savedVideos = savedVideoRepository.findVisibleByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                    .getContent()
                    .stream()
                    .map(SavedVideo::getVideo)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch recent saved videos for user profile calculation: {}", e.getMessage());
        }

        // Fetch recent views (limit to 100)
        List<VideoView> recentViews = List.of();
        try {
            recentViews = videoViewRepository.findRecentViewsWithRatesByViewerId(userId, PageRequest.of(0, 100));
        } catch (Exception e) {
            log.warn("Failed to fetch recent views for user profile calculation: {}", e.getMessage());
        }

        Map<String, Double> categoryCounts = new HashMap<>();
        Map<Long, Double> categoryIdCounts = new HashMap<>();
        Map<String, Double> hashtagCounts = new HashMap<>();
        Map<Long, Double> creatorCounts = new HashMap<>();

        // Aggregate saved videos (weight: 4.0)
        aggregateVideoList(categoryCounts, categoryIdCounts, hashtagCounts, creatorCounts, savedVideos, 4.0);

        // Aggregate liked videos (weight: 3.0)
        aggregateVideoList(categoryCounts, categoryIdCounts, hashtagCounts, creatorCounts, likedVideos, 3.0);

        // Aggregate viewed videos (weight based on completion rate)
        for (VideoView view : recentViews) {
            Video video = view.getVideo();
            if (video == null) continue;

            Double completion = view.getCompletionRate();
            double viewWeight = 0.5; // default low engagement view
            if (completion != null) {
                if (completion >= 0.9) {
                    viewWeight = 2.5; // very high engagement / watch completion
                } else if (completion >= 0.5) {
                    viewWeight = 1.0; // moderate engagement
                }
            }
            aggregateVideo(categoryCounts, categoryIdCounts, hashtagCounts, creatorCounts, video, viewWeight);
        }

        List<String> topCategories = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Long> topCategoryIds = categoryIdCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> topHashtags = hashtagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Long> favoriteCreatorIds = creatorCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Avoid categories
        List<String> avoidCategories = videoNotInterestedRepository.findAvoidCategoriesByUserId(userId);
        List<Long> avoidCategoryIds = videoNotInterestedRepository.findAvoidCategoryIdsByUserId(userId);

        // Avg watch completion
        Double avgCompletion = videoViewRepository.getAverageCompletionRateByViewerId(userId);
        if (avgCompletion == null) {
            avgCompletion = 0.0;
        }

        return UserInterestProfile.builder()
                .topCategories(topCategories)
                .topCategoryIds(topCategoryIds)
                .topHashtags(topHashtags)
                .favoriteCreatorIds(favoriteCreatorIds)
                .avoidCategories(avoidCategories)
                .avoidCategoryIds(avoidCategoryIds)
                .avgWatchCompletion(avgCompletion)
                .build();
    }

    private void aggregateVideoList(Map<String, Double> categoryCounts, Map<Long, Double> categoryIdCounts,
                                    Map<String, Double> hashtagCounts, Map<Long, Double> creatorCounts,
                                    List<Video> videos, double weight) {
        for (Video video : videos) {
            aggregateVideo(categoryCounts, categoryIdCounts, hashtagCounts, creatorCounts, video, weight);
        }
    }

    private void aggregateVideo(Map<String, Double> categoryCounts, Map<Long, Double> categoryIdCounts,
                                 Map<String, Double> hashtagCounts, Map<Long, Double> creatorCounts,
                                 Video video, double weight) {
        // Category
        String category = video.getAiCategory() != null ? video.getAiCategory() : video.getCategory();
        if (category != null && !category.isBlank()) {
            categoryCounts.merge(category, weight, Double::sum);
        }
        if (video.getVideoCategory() != null) {
            categoryIdCounts.merge(video.getVideoCategory().getId(), weight, Double::sum);
        }

        // Creator
        if (video.getUser() != null) {
            creatorCounts.merge(video.getUser().getId(), weight, Double::sum);
        }

        // Hashtags from description
        if (video.getDescription() != null) {
            Matcher matcher = HASHTAG_PATTERN.matcher(video.getDescription());
            while (matcher.find()) {
                String tag = matcher.group(1).toLowerCase();
                hashtagCounts.merge(tag, weight * 0.33, Double::sum); // Tag from desc gets 1/3 weight
            }
        }

        // AI Tags
        if (video.getAiTagsJson() != null && !video.getAiTagsJson().isBlank()) {
            try {
                List<String> aiTags = objectMapper.readValue(video.getAiTagsJson(), new TypeReference<List<String>>() {});
                for (String tag : aiTags) {
                    hashtagCounts.merge(tag.toLowerCase(), weight * 0.66, Double::sum); // AI tags get 2/3 weight
                }
            } catch (Exception ignored) {}
        }
    }
}
