package com.back.chat.service;

import com.back.chat.mapper.ConversationMapper;
import com.back.chat.model.dto.request.CreateConversationRequestDTO;
import com.back.chat.model.dto.response.ConversationResponseDTO;
import com.back.chat.model.dto.response.UnreadCountResponseDTO;
import com.back.chat.model.entity.Conversation;
import com.back.chat.model.entity.ConversationParticipant;
import com.back.chat.model.enums.ConversationStatus;
import com.back.chat.model.enums.ConversationType;
import com.back.chat.repo.IConversationParticipantRepository;
import com.back.chat.repo.IConversationRepository;
import com.back.chat.repo.IMessageRepository;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.repo.IFollowRepo;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl implements IChatConversationService {

    private final IConversationRepository IConversationRepository;
    private final IConversationParticipantRepository participantRepository;
    private final IMessageRepository messageRepository;
    private final IUserRepo userRepo;
    private final IFollowRepo followRepo;

    @Override
    public Page<ConversationResponseDTO> getMyConversations(Authentication authentication, ConversationStatus status, Pageable pageable) {
        User currentUser = getCurrentUser(authentication);
        Page<Conversation> conversations = status == ConversationStatus.REQUESTED
                ? participantRepository.findRequestConversationsByUser(
                        currentUser,
                        currentUser.getId(),
                        ConversationStatus.REQUESTED,
                        pageable
                )
                : participantRepository.findInboxConversationsByUser(
                        currentUser,
                        currentUser.getId(),
                        ConversationStatus.ACTIVE,
                        ConversationStatus.REQUESTED,
                        pageable
                );

        return conversations.map(conv -> {
            ConversationParticipant currentParticipant = participantRepository.findByConversationAndUser(conv, currentUser).orElse(null);
            ConversationParticipant targetParticipant = participantRepository.findTargetParticipant(conv, currentUser).orElse(null);
            return ConversationMapper.toResponse(
                    conv,
                    currentParticipant,
                    targetParticipant,
                    countUnreadIncomingMessages(conv, currentUser, currentParticipant)
            );
        });
    }

    @Override
    @Transactional
    public ConversationResponseDTO getOrCreateDirectConversation(Authentication authentication, CreateConversationRequestDTO request) {
        User currentUser = getCurrentUser(authentication);
        User targetUser = userRepo.findById(request.getTargetUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String directKey = generateDirectKey(currentUser.getId(), targetUser.getId());
        Optional<Conversation> existing = IConversationRepository.findByDirectKey(directKey);

        if (existing.isPresent()) {
            Conversation conv = existing.get();
            if (conv.getStatus() == ConversationStatus.REQUESTED && areFriends(currentUser, targetUser)) {
                conv.setStatus(ConversationStatus.ACTIVE);
                conv = IConversationRepository.save(conv);
            }
            ConversationParticipant currentParticipant = participantRepository.findByConversationAndUser(conv, currentUser).orElse(null);
            ConversationParticipant targetParticipant = participantRepository.findByConversationAndUser(conv, targetUser).orElse(null);
            return ConversationMapper.toResponse(
                    conv,
                    currentParticipant,
                    targetParticipant,
                    countUnreadIncomingMessages(conv, currentUser, currentParticipant)
            );
        }

        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .status(areFriends(currentUser, targetUser) ? ConversationStatus.ACTIVE : ConversationStatus.REQUESTED)
                .directKey(directKey)
                .createdBy(currentUser.getId())
                .build();
        conversation = IConversationRepository.save(conversation);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .conversation(conversation)
                .user(currentUser)
                .role("OWNER")
                .build();
        participantRepository.save(p1);

        ConversationParticipant p2 = ConversationParticipant.builder()
                .conversation(conversation)
                .user(targetUser)
                .role("MEMBER")
                .build();
        participantRepository.save(p2);

        return ConversationMapper.toResponse(conversation, p1, p2, 0L);
    }

    @Override
    @Transactional
    public void markAsRead(Authentication authentication, Long conversationId) {
        User currentUser = getCurrentUser(authentication);
        Conversation conversation = IConversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        participantRepository.findByConversationAndUser(conversation, currentUser).ifPresent(participant -> {
            participant.setLastReadAt(LocalDateTime.now());
            participant.setLastReadMessageId(conversation.getLastMessageId());
            participantRepository.save(participant);
        });
    }

    @Override
    public UnreadCountResponseDTO getUnreadCount(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        long totalUnread = participantRepository.countUnreadInboxConversations(
                currentUser,
                currentUser.getId(),
                ConversationStatus.ACTIVE,
                ConversationStatus.REQUESTED
        );
        long requestUnread = participantRepository.countUnreadRequestConversations(
                currentUser,
                currentUser.getId(),
                ConversationStatus.REQUESTED
        );
        return UnreadCountResponseDTO.builder()
                .totalUnread(totalUnread)
                .requestUnread(requestUnread)
                .build();
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

    private String generateDirectKey(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + ":" + max;
    }

    private boolean areFriends(User firstUser, User secondUser) {
        return followRepo.existsByFollowerAndFollowing(firstUser, secondUser)
                && followRepo.existsByFollowerAndFollowing(secondUser, firstUser);
    }

    private long countUnreadIncomingMessages(Conversation conversation, User currentUser, ConversationParticipant participant) {
        if (conversation == null || currentUser == null || participant == null) {
            return 0L;
        }
        return messageRepository.countUnreadIncomingMessages(conversation, currentUser, participant.getLastReadAt());
    }
}
