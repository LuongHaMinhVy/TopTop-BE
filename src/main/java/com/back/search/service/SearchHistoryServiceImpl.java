package com.back.search.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.search.mapper.SearchMapper;
import com.back.search.model.dto.request.SaveSearchHistoryRequestDTO;
import com.back.search.model.dto.response.SearchHistoryResponseDTO;
import com.back.search.model.entity.SearchHistory;
import com.back.search.model.entity.SearchKeywordStat;
import com.back.search.model.enums.SearchSourceType;
import com.back.search.model.enums.SearchType;
import com.back.search.repo.ISearchHistoryRepository;
import com.back.search.repo.ISearchKeywordStatRepository;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements ISearchHistoryService{
    private final ISearchHistoryRepository historyRepository;
    private final ISearchKeywordStatRepository keywordStatRepository;
    private final IUserRepo userRepo;

    @Override
    public List<SearchHistoryResponseDTO> getMyHistory(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return historyRepository.findByUserOrderBySearchedAtDesc(user, PageRequest.of(0, 20))
                .stream()
                .map(SearchMapper::toHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public SearchHistoryResponseDTO save(Authentication authentication, SaveSearchHistoryRequestDTO request) {
        User user = getCurrentUser(authentication);
        String normalized = normalizeKeyword(request.getKeyword());
        if (normalized.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        SearchType type = request.getType() == null ? SearchType.ALL : request.getType();
        SearchSourceType sourceType = request.getSourceType() == null
                ? SearchSourceType.KEYWORD
                : request.getSourceType();

        SearchHistory history = historyRepository
                .findByUserAndNormalizedKeywordAndType(user, normalized, type)
                .orElseGet(SearchHistory::new);
        history.setUser(user);
        history.setKeyword(request.getKeyword().trim());
        history.setNormalizedKeyword(normalized);
        history.setType(type);
        history.setSourceType(sourceType);
        history.setResultTargetId(request.getResultTargetId());
        history.setSearchedAt(now);

        incrementKeywordStat(request.getKeyword().trim(), normalized, now);
        return SearchMapper.toHistoryResponse(historyRepository.save(history));
    }

    @Override
    @Transactional
    public void deleteOne(Authentication authentication, Long historyId) {
        User user = getCurrentUser(authentication);
        SearchHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        if (!history.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        historyRepository.delete(history);
    }

    @Override
    @Transactional
    public void clear(Authentication authentication) {
        User user = getCurrentUser(authentication);
        historyRepository.deleteByUser(user);
    }

    private void incrementKeywordStat(String keyword, String normalized, LocalDateTime now) {
        SearchKeywordStat stat = keywordStatRepository.findByNormalizedKeyword(normalized)
                .orElseGet(() -> SearchKeywordStat.builder()
                        .keyword(keyword)
                        .normalizedKeyword(normalized)
                        .searchCount(0L)
                        .lastSearchedAt(now)
                        .build());

        stat.setKeyword(keyword);
        stat.setSearchCount((stat.getSearchCount() == null ? 0L : stat.getSearchCount()) + 1);
        stat.setLastSearchedAt(now);
        keywordStatRepository.save(stat);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
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
}
