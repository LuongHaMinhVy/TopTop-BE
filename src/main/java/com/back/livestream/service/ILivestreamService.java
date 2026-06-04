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

    /** Lightweight status-only check used by frontend polling loop. */
    LivestreamReadinessResponse getStreamReadiness(Long livestreamId);

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

    // ── Viewer count sync (called by webhook + join/leave) ────────────────────

    /** Increments viewerCount in DB and pushes real-time update via WebSocket. */
    void incrementViewerCount(Long livestreamId);

    /** Decrements viewerCount in DB (floor 0) and pushes real-time update via WebSocket. */
    void decrementViewerCount(Long livestreamId);

    /** Called when LiveKit room_finished event arrives — marks stream ENDED if not already. */
    void handleRoomFinished(Long livestreamId);

    /** Explicit viewer leave — records leftAt in participant table and decrements viewer count. */
    void leaveStream(Long livestreamId);

    /** Processes the raw LiveKit webhook event body, verifies signature, parses data, and updates status/viewer count. */
    void handleLivekitWebhook(String authHeader, String rawBody);
}
