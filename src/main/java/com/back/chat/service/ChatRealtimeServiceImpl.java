package com.back.chat.service;

import com.back.chat.model.dto.response.MessageRealtimeEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRealtimeServiceImpl implements IChatRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastMessage(Long conversationId, MessageRealtimeEventDTO event) {
        // Send to conversation room
        messagingTemplate.convertAndSend("/topic/chat." + conversationId, event);
        
        // Also send to individual participants for list updates if needed
        // messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", event);
    }
}
