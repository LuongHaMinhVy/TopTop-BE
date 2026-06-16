package com.back.video.service;

import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoCategory;
import com.back.video.repo.IVideoCategoryRepository;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoCategoryInitializer implements CommandLineRunner {

    private final IVideoCategoryRepository categoryRepository;
    private final IVideoRepository videoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("Initializing default video categories in database...");
            List<VideoCategory> defaultCategories = List.of(
                    VideoCategory.builder().code("COMEDY").name("Comedy").isActive(true).build(),
                    VideoCategory.builder().code("DANCE").name("Dance").isActive(true).build(),
                    VideoCategory.builder().code("MUSIC").name("Music").isActive(true).build(),
                    VideoCategory.builder().code("FOOD").name("Food").isActive(true).build(),
                    VideoCategory.builder().code("EDUCATION").name("Education").isActive(true).build(),
                    VideoCategory.builder().code("BEAUTY").name("Beauty").isActive(true).build(),
                    VideoCategory.builder().code("SPORTS").name("Sports").isActive(true).build(),
                    VideoCategory.builder().code("GAMING").name("Gaming").isActive(true).build(),
                    VideoCategory.builder().code("PETS").name("Pets").isActive(true).build(),
                    VideoCategory.builder().code("TRAVEL").name("Travel").isActive(true).build(),
                    VideoCategory.builder().code("DIY").name("DIY").isActive(true).build(),
                    VideoCategory.builder().code("NEWS").name("News").isActive(true).build(),
                    VideoCategory.builder().code("OTHER").name("Other").isActive(true).build()
            );
            categoryRepository.saveAll(defaultCategories);
            log.info("Default video categories initialized successfully!");
        }

        // Backfill legacy video category foreign keys
        List<Video> videosWithoutCategory = videoRepository.findByVideoCategoryIsNull();
        if (!videosWithoutCategory.isEmpty()) {
            log.info("Backfilling video category IDs for {} legacy videos...", videosWithoutCategory.size());
            for (Video video : videosWithoutCategory) {
                String catStr = video.getAiCategory() != null ? video.getAiCategory() : video.getCategory();
                if (catStr != null && !catStr.isBlank()) {
                    VideoCategory videoCategory = categoryRepository.findByCodeIgnoreCaseOrNameIgnoreCase(catStr, catStr)
                            .orElseGet(() -> categoryRepository.findByCodeIgnoreCase("OTHER").orElse(null));
                    video.setVideoCategory(videoCategory);
                } else {
                    VideoCategory otherCategory = categoryRepository.findByCodeIgnoreCase("OTHER").orElse(null);
                    video.setVideoCategory(otherCategory);
                }
            }
            videoRepository.saveAll(videosWithoutCategory);
            log.info("Successfully backfilled video category IDs for all legacy videos.");
        }
    }
}
