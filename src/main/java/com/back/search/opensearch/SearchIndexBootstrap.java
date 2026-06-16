package com.back.search.opensearch;

import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexBootstrap implements ApplicationRunner {
    private final OpenSearchSearchClient openSearchSearchClient;
    private final OpenSearchSearchProperties properties;
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.isIndexOnStartup()) {
            return;
        }

        openSearchSearchClient.ensureIndex();
        List<User> users = userRepo.findSearchIndexUsers();
        List<Video> videos = videoRepository.findSearchIndexVideos();
        openSearchSearchClient.bulkIndex(users, videos);
        log.info("Indexed {} users and {} videos into OpenSearch search index", users.size(), videos.size());
    }
}
