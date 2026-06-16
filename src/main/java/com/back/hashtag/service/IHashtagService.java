package com.back.hashtag.service;

import com.back.hashtag.model.dto.response.HashtagSuggestionResponseDTO;
import java.util.List;

public interface IHashtagService {
    List<HashtagSuggestionResponseDTO> getSuggestions(String keyword);
}
