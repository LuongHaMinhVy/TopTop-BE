package com.back.chat.service;

import com.back.chat.mapper.MessageMapper;
import com.back.chat.model.dto.request.SendMessageRequestDTO;
import com.back.chat.model.dto.response.MessageRealtimeEventDTO;
import com.back.chat.model.dto.response.MessageResponseDTO;
import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.Message;
import com.back.chat.model.entity.MessageAttachment;
import com.back.chat.model.enums.MessageStatus;
import com.back.chat.model.enums.MessageType;
import com.back.chat.repo.ConversationRepository;
import com.back.chat.repo.MessageAttachmentRepository;
import com.back.chat.repo.MessageRepository;
import com.back.chat.repo.ConversationParticipantRepository;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import com.back.video.model.entity.Video;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements IChatMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final IChatRealtimeService realtimeService;

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(Authentication authentication, SendMessageRequestDTO request) {
        User currentUser = getCurrentUser(authentication);
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        participantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ACCESS_DENIED));

        if (request.getType() == MessageType.VIDEO_SHARE && request.getVideoId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if ((request.getType() == MessageType.IMAGE || request.getType() == MessageType.VIDEO)
                && (request.getMediaUrl() == null || request.getMediaUrl().isBlank())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        messageRepository.findBySenderIdAndClientMessageId(currentUser.getId(), request.getClientMessageId())
                .ifPresent(m -> { throw new AppException(ErrorCode.BAD_REQUEST); });

        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .type(request.getType())
                .body(request.getBody())
                .status(MessageStatus.SENT)
                .clientMessageId(request.getClientMessageId())
                .replyToMessageId(request.getReplyToMessageId())
                .build();
        message = messageRepository.save(message);

        MessageAttachment attachment = null;
        if (request.getType() == MessageType.VIDEO_SHARE) {
            Video video = videoRepository.findById(request.getVideoId())
                    .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
            
            attachment = MessageAttachment.builder()
                    .message(message)
                    .type("VIDEO_POST")
                    .videoId(video.getId())
                    .url(video.getFileUrl())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .metadataJson(String.format(
                            "{\"title\":\"%s\",\"ownerUsername\":\"%s\"}",
                            escapeJson(video.getTitle()),
                            escapeJson(video.getUser().getUsername())
                    ))
                    .build();
            attachment = attachmentRepository.save(attachment);
        }
        if (request.getType() == MessageType.IMAGE || request.getType() == MessageType.VIDEO) {
            attachment = MessageAttachment.builder()
                    .message(message)
                    .type(request.getType().name())
                    .url(request.getMediaUrl().trim())
                    .thumbnailUrl(request.getType() == MessageType.IMAGE ? request.getMediaUrl().trim() : null)
                    .metadataJson(String.format(
                            "{\"fileName\":\"%s\",\"fileSize\":%d}",
                            escapeJson(request.getFileName()),
                            request.getFileSize() == null ? 0L : request.getFileSize()
                    ))
                    .build();
            attachment = attachmentRepository.save(attachment);
        }

        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageAt(LocalDateTime.now());
        String preview = switch (request.getType()) {
            case TEXT -> request.getBody();
            case VIDEO_SHARE -> request.getBody() != null && !request.getBody().isBlank()
                    ? request.getBody()
                    : "Đã chia sẻ một video";
            case IMAGE -> request.getBody() != null && !request.getBody().isBlank()
                    ? request.getBody()
                    : "Đã gửi một ảnh";
            case VIDEO -> request.getBody() != null && !request.getBody().isBlank()
                    ? request.getBody()
                    : "Đã gửi một video";
            default -> "Sent an attachment";
        };
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        MessageResponseDTO responseDTO = MessageMapper.toResponse(message, attachment, currentUser.getId());

        realtimeService.broadcastMessage(conversation.getId(), MessageRealtimeEventDTO.builder()
                .event("chat.message.new")
                .conversationId(conversation.getId())
                .message(responseDTO)
                .occurredAt(LocalDateTime.now())
                .build());

        return responseDTO;
    }

    @Override
    public Page<MessageResponseDTO> getMessages(Authentication authentication, Long conversationId, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        participantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ACCESS_DENIED));

        Page<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);

        return messages.map(m -> {
            MessageAttachment attachment = attachmentRepository.findByMessage(m).orElse(null);
            return MessageMapper.toResponse(m, attachment, currentUser.getId());
        });
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
