package com.back.moderation.service;

import com.back.video.model.entity.Video;

public interface IMusicCopyrightService {
    MusicCopyrightResult check(Video video);
}
