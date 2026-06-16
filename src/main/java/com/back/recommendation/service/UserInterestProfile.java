package com.back.recommendation.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<String> topCategories = new ArrayList<>();

    @Builder.Default
    private List<Long> topCategoryIds = new ArrayList<>();

    @Builder.Default
    private List<String> topHashtags = new ArrayList<>();

    @Builder.Default
    private List<Long> favoriteCreatorIds = new ArrayList<>();

    @Builder.Default
    private List<String> avoidCategories = new ArrayList<>();

    @Builder.Default
    private List<Long> avoidCategoryIds = new ArrayList<>();

    @Builder.Default
    private double avgWatchCompletion = 0.0;
}
