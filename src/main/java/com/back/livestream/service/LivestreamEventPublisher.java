package com.back.livestream.service;

import com.back.livestream.model.dto.response.LiveChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivestreamEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_CHAT    = "/topic/lives/{id}/chat";
    private static final String TOPIC_EVENTS  = "/topic/lives/{id}/events";
    private static final String TOPIC_VIEWERS = "/topic/lives/{id}/viewer-count";

    public void publishChat(Long livestreamId, LiveChatMessageResponse msg) {
        String dest = TOPIC_CHAT.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CHAT");
        payload.put("data", msg);
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }

    public void publishGift(Long livestreamId, Object giftEvent) {
        String dest = TOPIC_EVENTS.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "GIFT");
        payload.put("data", giftEvent);
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }

    public void publishReaction(Long livestreamId, String reactionType, long count) {
        String dest = TOPIC_EVENTS.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "REACTION");
        payload.put("reactionType", reactionType);
        payload.put("count", count);
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }

    public void publishViewerCount(Long livestreamId, int viewerCount) {
        String dest = TOPIC_VIEWERS.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "VIEWER_COUNT");
        payload.put("viewerCount", viewerCount);
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }

    public void publishModerationEvent(Long livestreamId, String action, Long messageId, Long targetUserId) {
        String dest = TOPIC_EVENTS.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", action);
        if (messageId != null) payload.put("messageId", messageId);
        if (targetUserId != null) payload.put("targetUserId", targetUserId);
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }

    public void publishStreamEnded(Long livestreamId) {
        String dest = TOPIC_EVENTS.replace("{id}", String.valueOf(livestreamId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "STREAM_ENDED");
        messagingTemplate.convertAndSend(dest, (Object) payload);
    }
}
