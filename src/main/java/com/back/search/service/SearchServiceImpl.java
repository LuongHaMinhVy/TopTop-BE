package com.back.search.service;

import com.back.follow.repo.IFollowRepo;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService{
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final IFollowRepo followRepo;
    private final ISearchHistoryRepository historyRepository;
    private final ISearchKeywordStatRepository keywordStatRepository;

    @Override
    public SearchTopResponseDTO searchTop(String keyword, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);

        return SearchTopResponseDTO.builder()
                .videos(videoRepository.searchPublicVideos(normalized, PageRequest.of(0, 4))
                        .map(video -> SearchMapper.toVideoResponse(video, viewer, followRepo))
                        .getContent())
                .users(userRepo.searchUsers(normalized, PageRequest.of(0, 4))
                        .map(user -> SearchMapper.toUserResponse(user, viewer, followRepo))
                        .getContent())
                .hashtags(List.of())
                .lives(List.of())
                .relatedSearches(relatedSearches(normalized, 8))
                .build();
    }

    @Override
    public Page<SearchUserResponseDTO> searchUsers(String keyword, Pageable pageable, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);
        return userRepo.searchUsers(normalized, capPageable(pageable))
                .map(user -> SearchMapper.toUserResponse(user, viewer, followRepo));
    }

    @Override
    public Page<SearchVideoResponseDTO> searchVideos(String keyword, Pageable pageable, Authentication authentication) {
        String normalized = normalizeKeyword(keyword);
        User viewer = getOptionalCurrentUser(authentication).orElse(null);
        return videoRepository.searchPublicVideos(normalized, capPageable(pageable))
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

        Page<User> userMatches = userRepo.searchUsers(normalized, PageRequest.of(0, 5));
        userMatches.stream().map(User::getUsername).forEach(keywords::add);

        videoRepository.searchPublicVideos(normalized, PageRequest.of(0, 5))
                .stream()
                .map(Video::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .forEach(keywords::add);

        return SearchSuggestionResponseDTO.builder()
                .keywords(keywords.stream().limit(8).toList())
                .users(userMatches.map(user -> SearchMapper.toUserResponse(user, viewer, followRepo)).getContent())
                .hashtags(List.of())
                .relatedSearches(relatedSearches(normalized, 6))
                .build();
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
