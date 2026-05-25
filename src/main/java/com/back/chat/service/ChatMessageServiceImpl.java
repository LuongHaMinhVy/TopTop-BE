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
import com.back.chat.repo.IConversationRepository;
import com.back.chat.repo.IMessageAttachmentRepository;
import com.back.chat.repo.IMessageRepository;
import com.back.chat.repo.IConversationParticipantRepository;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.moderation.service.ITextContentModerationService;
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

    private final IMessageRepository IMessageRepository;
    private final IConversationRepository IConversationRepository;
    private final IConversationParticipantRepository participantRepository;
    private final IMessageAttachmentRepository attachmentRepository;
    private final IUserRepo userRepo;
    private final IVideoRepository videoRepository;
    private final IChatRealtimeService realtimeService;
    private final ITextContentModerationService textContentModerationService;

    @Override
    @Transactional
    public MessageResponseDTO sendMessage(Authentication authentication, SendMessageRequestDTO request) {
        User currentUser = getCurrentUser(authentication);
        Conversation conversation = IConversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        var currentParticipant = participantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ACCESS_DENIED));

        if (request.getType() == MessageType.VIDEO_SHARE && request.getVideoId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if ((request.getType() == MessageType.IMAGE || request.getType() == MessageType.VIDEO)
                && (request.getMediaUrl() == null || request.getMediaUrl().isBlank())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        String body = normalizeBody(request.getBody());
        textContentModerationService.assertAllowed("MESSAGE", body, currentUser.getId(), "body");

        IMessageRepository.findBySenderIdAndClientMessageId(currentUser.getId(), request.getClientMessageId())
                .ifPresent(m -> { throw new AppException(ErrorCode.BAD_REQUEST); });

        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .type(request.getType())
                .body(body)
                .status(MessageStatus.SENT)
                .clientMessageId(request.getClientMessageId())
                .replyToMessageId(request.getReplyToMessageId())
                .build();
        message = IMessageRepository.save(message);

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
            case TEXT -> body;
            case VIDEO_SHARE -> body != null && !body.isBlank()
                    ? body
                    : "Đã chia sẻ một video";
            case IMAGE -> body != null && !body.isBlank()
                    ? body
                    : "Đã gửi một ảnh";
            case VIDEO -> body != null && !body.isBlank()
                    ? body
                    : "Đã gửi một video";
            default -> "Sent an attachment";
        };
        conversation.setLastMessagePreview(preview);
        IConversationRepository.save(conversation);

        currentParticipant.setLastReadAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        currentParticipant.setLastReadMessageId(message.getId());
        participantRepository.save(currentParticipant);

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
        Conversation conversation = IConversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        participantRepository.findByConversationAndUser(conversation, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ACCESS_DENIED));

        Page<Message> messages = IMessageRepository.findVisibleByConversationForUser(
                conversation,
                hiddenToken(currentUser.getId()),
                pageable
        );

        return messages.map(m -> {
            MessageAttachment attachment = attachmentRepository.findByMessage(m).orElse(null);
            return MessageMapper.toResponse(m, attachment, currentUser.getId());
        });
    }

    @Override
    @Transactional
    public void deleteMessage(Authentication authentication, Long messageId) {
        User currentUser = getCurrentUser(authentication);
        Message message = IMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        participantRepository.findByConversationAndUser(message.getConversation(), currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ACCESS_DENIED));

        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        message.setHiddenForUserIds(addHiddenUser(message.getHiddenForUserIds(), currentUser.getId()));
        IMessageRepository.save(message);
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

    private String normalizeBody(String body) {
        String normalized = body == null ? "" : body.trim();
        if (normalized.length() > 4000) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String hiddenToken(Long userId) {
        return "%," + userId + ",%";
    }

    private String addHiddenUser(String hiddenForUserIds, Long userId) {
        String token = "," + userId + ",";
        if (hiddenForUserIds != null && hiddenForUserIds.contains(token)) {
            return hiddenForUserIds;
        }
        if (hiddenForUserIds == null || hiddenForUserIds.isBlank()) {
            return token;
        }
        return hiddenForUserIds + userId + ",";
    }
}
