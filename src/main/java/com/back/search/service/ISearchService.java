package com.back.search.service;

import com.back.search.model.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface ISearchService{
    SearchTopResponseDTO searchTop(String keyword, Authentication authentication);

    Page<SearchUserResponseDTO> searchUsers(String keyword, Pageable pageable, Authentication authentication);

    Page<SearchVideoResponseDTO> searchVideos(String keyword, Pageable pageable, Authentication authentication);

    Page<SearchLiveResponseDTO> searchLive(String keyword, Pageable pageable, Authentication authentication);

    SearchSuggestionResponseDTO suggestions(String keyword, Authentication authentication);
}
