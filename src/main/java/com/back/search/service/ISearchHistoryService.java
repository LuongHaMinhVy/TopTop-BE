package com.back.search.service;

import com.back.search.model.dto.request.SaveSearchHistoryRequestDTO;
import com.back.search.model.dto.response.SearchHistoryResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface ISearchHistoryService{
    List<SearchHistoryResponseDTO> getMyHistory(Authentication authentication);

    SearchHistoryResponseDTO save(Authentication authentication, SaveSearchHistoryRequestDTO request);

    void deleteOne(Authentication authentication, Long historyId);

    void clear(Authentication authentication);
}
