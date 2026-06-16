package com.back.recommendation.service;

import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;

import java.util.List;

public interface IVideoRecommendationService {
    List<Video> rankForYou(List<Video> candidates, User viewer);
}
