package com.back.chat.service;

import com.back.chat.model.dto.response.MessageRealtimeEventDTO;

public interface IChatRealtimeService {
    void broadcastMessage(Long conversationId, MessageRealtimeEventDTO event);
}
