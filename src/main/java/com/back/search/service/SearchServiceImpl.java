package com.back.search.service;

import com.back.follow.repo.IFollowRepo;
import com.back.search.opensearch.OpenSearchSearchClient;
import com.back.search.opensearch.SearchDocumentType;
import com.back.search.mapper.SearchMapper;
import com.back.search.model.dto.response.*;
import com.back.search.model.entity.SearchHistory;
import com.back.search.model.entity.SearchKeywordStat;
import com.back.search.repo.ISearchHistoryRepository;
import com.back.search.repo.ISearchKeywordStatRepository;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService{
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final IFollowRepo followRepo;
    private final ISearchHistoryRepository historyRepository;
    private final ISearchKeywordStatRepository keywordStatRepository;
    private final OpenSearchSearchClient openSearchSearchClient;

    @Override
    public SearchTopResponseDTO searchTop(String keyword, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);

        return SearchTopResponseDTO.builder()
                .videos(searchVideos(normalized, PageRequest.of(0, 4), authentication).getContent())
                .users(searchUsers(normalized, PageRequest.of(0, 4), authentication).getContent())
                .hashtags(List.of())
                .lives(List.of())
                .relatedSearches(relatedSearches(normalized, 8))
                .build();
    }

    @Override
    public Page<SearchUserResponseDTO> searchUsers(String keyword, Pageable pageable, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);
        Pageable capped = capPageable(pageable);

        Optional<OpenSearchSearchClient.OpenSearchPage> openSearchPage =
                openSearchSearchClient.searchIds(SearchDocumentType.USER, normalized, capped);
        if (openSearchPage.isPresent()) {
            List<User> users = orderedUsers(openSearchPage.get().ids());
            return new PageImpl<>(
                    users.stream().map(user -> SearchMapper.toUserResponse(user, viewer, followRepo)).toList(),
                    capped,
                    openSearchPage.get().total()
            );
        }

        return userRepo.searchUsers(normalized, capPageable(pageable))
                .map(user -> SearchMapper.toUserResponse(user, viewer, followRepo));
    }

    @Override
    public Page<SearchVideoResponseDTO> searchVideos(String keyword, Pageable pageable, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);
        Pageable capped = capPageable(pageable);

        Optional<OpenSearchSearchClient.OpenSearchPage> openSearchPage =
                openSearchSearchClient.searchIds(SearchDocumentType.VIDEO, normalized, capped);
        if (openSearchPage.isPresent()) {
            List<Video> videos = orderedVideos(openSearchPage.get().ids());
            return new PageImpl<>(
                    videos.stream().map(video -> SearchMapper.toVideoResponse(video, viewer, followRepo)).toList(),
                    capped,
                    openSearchPage.get().total()
            );
        }

        return videoRepository.searchPublicVideos(normalized, capped)
                .map(video -> SearchMapper.toVideoResponse(video, viewer, followRepo));
    }

    @Override
    public Page<SearchLiveResponseDTO> searchLive(String keyword, Pageable pageable, Authentication authentication) {
        return new PageImpl<>(List.of(), capPageable(pageable), 0);
    }

    @Override
    public SearchSuggestionResponseDTO suggestions(String keyword, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);
        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        if (viewer != null) {
            historyRepository
                    .findByUserAndNormalizedKeywordContainingIgnoreCaseOrderBySearchedAtDesc(
                            viewer,
                            normalized,
                            PageRequest.of(0, 6)
                    )
                    .stream()
                    .map(SearchHistory::getKeyword)
                    .forEach(keywords::add);
        }

        keywordStatRepository
                .findByNormalizedKeywordContainingIgnoreCaseOrderBySearchCountDescLastSearchedAtDesc(
                        normalized,
                        PageRequest.of(0, 8)
                )
                .stream()
                .map(SearchKeywordStat::getKeyword)
                .forEach(keywords::add);

        openSearchSearchClient.suggestKeywords(normalized, 8).forEach(keywords::add);

        Page<User> userMatches = userRepo.searchUsers(normalized, PageRequest.of(0, 5));
        userMatches.stream().map(User::getUsername).forEach(keywords::add);

        videoRepository.searchPublicVideos(normalized, PageRequest.of(0, 5))
                .stream()
                .map(Video::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .forEach(keywords::add);

        String didYouMean = openSearchSearchClient.suggestCorrection(normalized)
                .or(() -> closestCorrection(normalized, new ArrayList<>(keywords)))
                .orElse(null);

        return SearchSuggestionResponseDTO.builder()
                .keywords(keywords.stream().limit(8).toList())
                .didYouMean(didYouMean)
                .users(userMatches.map(user -> SearchMapper.toUserResponse(user, viewer, followRepo)).getContent())
                .hashtags(List.of())
                .relatedSearches(relatedSearches(normalized, 6))
                .build();
    }

    private List<User> orderedUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = userRepo.findPublicSearchUsersByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ids.stream().map(usersById::get).filter(java.util.Objects::nonNull).toList();
    }

    private List<Video> orderedVideos(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Video> videosById = videoRepository.findPublicSearchVideosByIds(ids).stream()
                .collect(Collectors.toMap(Video::getId, Function.identity()));
        return ids.stream().map(videosById::get).filter(java.util.Objects::nonNull).toList();
    }

    private List<RelatedSearchResponseDTO> relatedSearches(String normalized, int limit) {
        List<SearchKeywordStat> stats = normalized.isBlank()
                ? keywordStatRepository.findByOrderBySearchCountDescLastSearchedAtDesc(PageRequest.of(0, limit))
                : keywordStatRepository.findByNormalizedKeywordContainingIgnoreCaseOrderBySearchCountDescLastSearchedAtDesc(
                        normalized,
                        PageRequest.of(0, limit)
                );

        return stats.stream()
                .map(stat -> RelatedSearchResponseDTO.builder()
                        .keyword(stat.getKeyword())
                        .searchCount(stat.getSearchCount())
                        .build())
                .toList();
    }

    private Pageable capPageable(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, pageable.getSort());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Optional<String> closestCorrection(String normalized, List<String> candidates) {
        if (normalized.length() < 3 || candidates.isEmpty()) {
            return Optional.empty();
        }

        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            String normalizedCandidate = normalizeKeyword(candidate);
            if (normalizedCandidate.isBlank() || normalizedCandidate.equals(normalized)) {
                continue;
            }

            int distance = Math.min(
                    levenshtein(normalized, normalizedCandidate),
                    List.of(normalizedCandidate.split("\\s+")).stream()
                            .mapToInt(token -> levenshtein(normalized, token))
                            .min()
                            .orElse(Integer.MAX_VALUE)
            );

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        int allowedDistance = Math.max(1, Math.min(3, normalized.length() / 3));
        return best != null && bestDistance <= allowedDistance ? Optional.of(best) : Optional.empty();
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }

        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int cost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private Optional<User> getOptionalCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email);
    }
}
