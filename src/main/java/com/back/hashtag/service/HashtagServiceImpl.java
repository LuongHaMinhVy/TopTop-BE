package com.back.hashtag.service;

import com.back.hashtag.model.dto.response.HashtagSuggestionResponseDTO;
import com.back.hashtag.model.entity.Hashtag;
import com.back.hashtag.repo.IHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HashtagServiceImpl implements IHashtagService {

    private final IHashtagRepository hashtagRepository;

    @Override
    public List<HashtagSuggestionResponseDTO> getSuggestions(String keyword) {
        List<Hashtag> hashtags;
        if (keyword == null || keyword.trim().isEmpty()) {
            hashtags = hashtagRepository.findTop10ByOrderByPostCountDesc();
        } else {
            hashtags = hashtagRepository.findTop10ByNameContainingIgnoreCaseOrderByPostCountDesc(keyword.trim());
        }

        return hashtags.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private HashtagSuggestionResponseDTO mapToDTO(Hashtag hashtag) {
        return HashtagSuggestionResponseDTO.builder()
                .id(hashtag.getId())
                .name(hashtag.getName())
                .postCount(hashtag.getPostCount())
                .formattedPostCount(formatCount(hashtag.getPostCount()))
                .build();
    }

    private String formatCount(Long count) {
        if (count == null || count == 0) return "0";
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) {
            double result = count / 1000.0;
            return (result == Math.floor(result) ? String.format("%.0fK", result) : String.format("%.1fK", result));
        }
        double result = count / 1000000.0;
        return (result == Math.floor(result) ? String.format("%.0fM", result) : String.format("%.1fM", result));
    }
}
