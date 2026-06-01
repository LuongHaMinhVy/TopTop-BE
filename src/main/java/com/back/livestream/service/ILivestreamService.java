package com.back.livestream.service;

import com.back.livestream.model.dto.request.CreateLivestreamRequest;
import com.back.livestream.model.dto.request.SendChatMessageRequest;
import com.back.livestream.model.dto.request.SendGiftRequest;
import com.back.livestream.model.dto.response.*;

import java.util.List;

public interface ILivestreamService {

    LivestreamResponse createLivestream(CreateLivestreamRequest request);

    JoinLivestreamResponse startLivestream(Long livestreamId);

    JoinLivestreamResponse joinLivestream(Long livestreamId);

    void endLivestream(Long livestreamId);

    LivestreamResponse getLivestream(Long livestreamId);

    List<LivestreamResponse> getLiveFeed(int page, int size);

    LiveChatMessageResponse sendChatMessage(Long livestreamId, SendChatMessageRequest request);

    List<LiveChatMessageResponse> getChatHistory(Long livestreamId, int page, int size);

    void sendReaction(Long livestreamId, String type);

    void sendGift(Long livestreamId, SendGiftRequest request);

    List<GiftCatalogResponse> getGiftCatalog();

    // Moderation
    void hideChatMessage(Long livestreamId, Long messageId);

    void pinChatMessage(Long livestreamId, Long messageId);

    void banUser(Long livestreamId, Long userId, String reason);

    List<LivestreamResponse> getMyLivestreams();
}
