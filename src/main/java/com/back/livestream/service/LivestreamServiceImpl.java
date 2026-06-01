package com.back.livestream.service;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.follow.repo.IFollowRepo;
import com.back.livestream.model.dto.request.CreateLivestreamRequest;
import com.back.livestream.model.dto.request.SendChatMessageRequest;
import com.back.livestream.model.dto.request.SendGiftRequest;
import com.back.livestream.model.dto.response.*;
import com.back.livestream.model.entity.*;
import com.back.livestream.model.enums.*;
import com.back.livestream.repo.*;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivestreamServiceImpl implements ILivestreamService {

    private final ILivestreamRepo livestreamRepo;
    private final ILivestreamParticipantRepo participantRepo;
    private final ILivestreamChatMessageRepo chatRepo;
    private final ILivestreamReactionRepo reactionRepo;
    private final ILivestreamGiftRepo giftRepo;
    private final IGiftCatalogRepo giftCatalogRepo;
    private final ILivestreamModeratorRepo moderatorRepo;
    private final ILivestreamBanRepo banRepo;
    private final ILivestreamModerationLogRepo modLogRepo;
    private final IUserRepo userRepo;
    private final IFollowRepo followRepo;
    private final LiveKitTokenService liveKitTokenService;
    private final LivestreamEventPublisher eventPublisher;

    // ─── Auth helper ─────────────────────────────────────────────────────────

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        String email;
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = auth.getName();
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User requireCurrentUser() {
        User u = getCurrentUser();
        if (u == null) throw new AppException(ErrorCode.UNAUTHORIZED);
        return u;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Livestream requireLivestream(Long id) {
        return livestreamRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LIVESTREAM_NOT_FOUND));
    }

    private boolean isModerator(Livestream ls, User user) {
        return moderatorRepo.existsByLivestreamAndUser(ls, user);
    }

    private boolean isHostOrModerator(Livestream ls, User user) {
        return ls.getHost().getId().equals(user.getId()) || isModerator(ls, user);
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName().name()));
    }

    private String buildRoomName(Livestream ls) {
        return "live_" + ls.getHost().getId() + "_" + ls.getId();
    }

    private String resolveDisplayName(User user) {
        String nick = user.getNickname();
        return (nick != null && !nick.isBlank()) ? nick : user.getUsername();
    }

    private LivestreamResponse mapToResponse(Livestream ls, User viewer) {
        boolean isFollowing = false;
        if (viewer != null && !viewer.getId().equals(ls.getHost().getId())) {
            isFollowing = followRepo.existsByFollowerAndFollowing(viewer, ls.getHost());
        }
        return LivestreamResponse.builder()
                .id(ls.getId())
                .title(ls.getTitle())
                .description(ls.getDescription())
                .thumbnailUrl(ls.getThumbnailUrl())
                .status(ls.getStatus())
                .visibility(ls.getVisibility())
                .allowChat(ls.isAllowChat())
                .allowGifts(ls.isAllowGifts())
                .roomName(ls.getRoomName())
                .viewerCount(ls.getViewerCount())
                .likeCount(ls.getLikeCount())
                .giftCount(ls.getGiftCount())
                .categoryName(ls.getCategory() != null ? ls.getCategory().getName() : null)
                .host(HostSummary.builder()
                        .id(ls.getHost().getId())
                        .username(ls.getHost().getUsername())
                        .displayName(resolveDisplayName(ls.getHost()))
                        .avatarUrl(ls.getHost().getAvatarUrl())
                        .isFollowing(isFollowing)
                        .build())
                .startedAt(ls.getStartedAt())
                .createdAt(ls.getCreatedAt())
                .build();
    }

    private LiveChatMessageResponse mapToChat(LivestreamChatMessage msg) {
        return LiveChatMessageResponse.builder()
                .id(msg.getId())
                .livestreamId(msg.getLivestream().getId())
                .type(msg.getMessageType())
                .sender(LiveChatMessageResponse.SenderSummary.builder()
                        .id(msg.getSender().getId())
                        .username(msg.getSender().getUsername())
                        .displayName(resolveDisplayName(msg.getSender()))
                        .avatarUrl(msg.getSender().getAvatarUrl())
                        .build())
                .message(msg.getMessage())
                .isPinned(msg.isPinned())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private JoinLivestreamResponse buildJoinResponse(Livestream ls, User user, ParticipantRole role) {
        String token;
        if (role == ParticipantRole.HOST) {
            token = liveKitTokenService.generateHostToken(
                    ls.getRoomName(), String.valueOf(user.getId()), resolveDisplayName(user));
        } else if (role == ParticipantRole.MODERATOR) {
            token = liveKitTokenService.generateModeratorToken(
                    ls.getRoomName(), String.valueOf(user.getId()), resolveDisplayName(user));
        } else {
            token = liveKitTokenService.generateViewerToken(
                    ls.getRoomName(), String.valueOf(user.getId()), resolveDisplayName(user));
        }

        String livekitUrl = liveKitTokenService.getLivekitUrl();
        log.info("Issuing LiveKit {} token for livestream {} room {} via {}",
                role, ls.getId(), ls.getRoomName(), livekitUrl);

        return JoinLivestreamResponse.builder()
                .livestreamId(ls.getId())
                .roomName(ls.getRoomName())
                .livekitUrl(livekitUrl)
                .token(token)
                .role(role)
                .build();
    }

    // ─── Livestream lifecycle ────────────────────────────────────────────────

    @Override
    @Transactional
    public LivestreamResponse createLivestream(CreateLivestreamRequest request) {
        User host = requireCurrentUser();

        // One active stream per user
        if (livestreamRepo.existsByHostAndStatus(host, LivestreamStatus.LIVE)) {
            throw new AppException(ErrorCode.LIVESTREAM_ALREADY_ACTIVE);
        }

        Livestream ls = Livestream.builder()
                .host(host)
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .visibility(request.getVisibility() != null ? request.getVisibility() : LivestreamVisibility.PUBLIC)
                .allowChat(request.isAllowChat())
                .allowGifts(request.isAllowGifts())
                .status(LivestreamStatus.SCHEDULED)
                .build();

        if (request.getCategoryId() != null) {
            log.debug("Category id {} requested", request.getCategoryId());
        }

        ls = livestreamRepo.save(ls);
        ls.setRoomName(buildRoomName(ls));
        ls = livestreamRepo.save(ls);

        return mapToResponse(ls, host);
    }

    @Override
    @Transactional
    public JoinLivestreamResponse startLivestream(Long livestreamId) {
        User host = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);

        if (!ls.getHost().getId().equals(host.getId()) && !isAdmin(host)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (ls.getStatus() == LivestreamStatus.ENDED || ls.getStatus() == LivestreamStatus.CANCELLED) {
            throw new AppException(ErrorCode.LIVESTREAM_ENDED);
        }

        if (ls.getRoomName() == null) {
            ls.setRoomName(buildRoomName(ls));
        }
        if (ls.getStatus() != LivestreamStatus.LIVE) {
            ls.setStatus(LivestreamStatus.LIVE);
            ls.setStartedAt(LocalDateTime.now());
        }
        ls = livestreamRepo.save(ls);

        // Record host participant
        LivestreamParticipant participant = participantRepo
                .findFirstByLivestreamAndUserOrderByIdDesc(ls, host)
                .orElse(LivestreamParticipant.builder()
                        .livestream(ls)
                        .user(host)
                        .role(ParticipantRole.HOST)
                        .build());
        participant.setJoinedAt(LocalDateTime.now());
        participant.setRole(ParticipantRole.HOST);
        participantRepo.save(participant);

        return buildJoinResponse(ls, host, ParticipantRole.HOST);
    }

    @Override
    @Transactional
    public JoinLivestreamResponse joinLivestream(Long livestreamId) {
        User viewer = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);

        if (ls.getStatus() != LivestreamStatus.LIVE) {
            throw new AppException(ErrorCode.LIVESTREAM_NOT_LIVE);
        }

        // Check visibility
        if (ls.getVisibility() == LivestreamVisibility.PRIVATE && !ls.getHost().getId().equals(viewer.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (banRepo.existsByLivestreamAndUser(ls, viewer)) {
            throw new AppException(ErrorCode.USER_BANNED_FROM_LIVE);
        }

        // Upsert participant
        LivestreamParticipant participant = participantRepo
                .findFirstByLivestreamAndUserOrderByIdDesc(ls, viewer)
                .orElse(LivestreamParticipant.builder()
                        .livestream(ls)
                        .user(viewer)
                        .role(ParticipantRole.VIEWER)
                        .build());
        participant.setJoinedAt(LocalDateTime.now());
        participant.setLeftAt(null);
        participantRepo.save(participant);

        // Determine role
        ParticipantRole role = ls.getHost().getId().equals(viewer.getId())
                ? ParticipantRole.HOST
                : (isModerator(ls, viewer) ? ParticipantRole.MODERATOR : ParticipantRole.VIEWER);

        return buildJoinResponse(ls, viewer, role);
    }

    @Override
    @Transactional
    public void endLivestream(Long livestreamId) {
        User user = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);

        if (!ls.getHost().getId().equals(user.getId()) && !isAdmin(user)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (ls.getStatus() == LivestreamStatus.ENDED || ls.getStatus() == LivestreamStatus.CANCELLED) {
            return; // idempotent
        }

        ls.setStatus(LivestreamStatus.ENDED);
        ls.setEndedAt(LocalDateTime.now());
        livestreamRepo.save(ls);

        eventPublisher.publishStreamEnded(livestreamId);
    }

    // ─── Feed ────────────────────────────────────────────────────────────────

    @Override
    public List<LivestreamResponse> getLiveFeed(int page, int size) {
        User viewer = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        if (viewer == null) {
            return livestreamRepo.findPublicLiveFeed(LivestreamStatus.LIVE, pageable)
                    .stream().map(ls -> mapToResponse(ls, null)).collect(Collectors.toList());
        }
        return livestreamRepo.findVisibleFeedForUser(LivestreamStatus.LIVE, viewer, pageable)
                .stream().map(ls -> mapToResponse(ls, viewer)).collect(Collectors.toList());
    }

    @Override
    public LivestreamResponse getLivestream(Long livestreamId) {
        User viewer = getCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        return mapToResponse(ls, viewer);
    }

    @Override
    public List<LivestreamResponse> getMyLivestreams() {
        User user = requireCurrentUser();
        return livestreamRepo.findAllByHostOrderByCreatedAtDesc(user)
                .stream()
                .map(ls -> mapToResponse(ls, user))
                .collect(Collectors.toList());
    }

    // ─── Chat ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LiveChatMessageResponse sendChatMessage(Long livestreamId, SendChatMessageRequest request) {
        User sender = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);

        if (ls.getStatus() != LivestreamStatus.LIVE) throw new AppException(ErrorCode.LIVESTREAM_NOT_LIVE);
        if (!ls.isAllowChat()) throw new AppException(ErrorCode.CHAT_DISABLED);
        if (banRepo.existsByLivestreamAndUser(ls, sender)) throw new AppException(ErrorCode.USER_BANNED_FROM_LIVE);

        String sanitized = request.getMessage().trim();
        if (sanitized.isBlank()) throw new AppException(ErrorCode.BAD_REQUEST);

        LivestreamChatMessage msg = LivestreamChatMessage.builder()
                .livestream(ls)
                .sender(sender)
                .message(sanitized)
                .messageType(ChatMessageType.CHAT)
                .build();
        msg = chatRepo.save(msg);

        LiveChatMessageResponse resp = mapToChat(msg);
        eventPublisher.publishChat(livestreamId, resp);
        return resp;
    }

    @Override
    public List<LiveChatMessageResponse> getChatHistory(Long livestreamId, int page, int size) {
        Livestream ls = requireLivestream(livestreamId);
        Pageable pageable = PageRequest.of(page, size);
        return chatRepo.findVisibleMessages(ls, pageable)
                .stream().map(this::mapToChat).collect(Collectors.toList());
    }

    // ─── Reactions ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void sendReaction(Long livestreamId, String type) {
        User user = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        if (ls.getStatus() != LivestreamStatus.LIVE) throw new AppException(ErrorCode.LIVESTREAM_NOT_LIVE);

        String reactionType = "LIKE"; // MVP: only LIKE
        Optional<LivestreamReaction> existing = reactionRepo.findByLivestreamAndUserAndType(ls, user, reactionType);
        if (existing.isPresent()) {
            LivestreamReaction r = existing.get();
            r.setCount(r.getCount() + 1);
            reactionRepo.save(r);
        } else {
            reactionRepo.save(LivestreamReaction.builder()
                    .livestream(ls).user(user).type(reactionType).count(1).build());
        }

        ls.setLikeCount(ls.getLikeCount() + 1);
        livestreamRepo.save(ls);

        long total = reactionRepo.sumCountByLivestreamAndType(ls, reactionType);
        eventPublisher.publishReaction(livestreamId, reactionType, total);
    }

    // ─── Gifts ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void sendGift(Long livestreamId, SendGiftRequest request) {
        User sender = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        if (ls.getStatus() != LivestreamStatus.LIVE) throw new AppException(ErrorCode.LIVESTREAM_NOT_LIVE);
        if (!ls.isAllowGifts()) throw new AppException(ErrorCode.GIFTS_DISABLED);

        GiftCatalog gift = giftCatalogRepo.findById(request.getGiftId())
                .filter(GiftCatalog::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.GIFT_NOT_FOUND));

        int qty = Math.max(1, request.getQuantity());
        int total = gift.getCoinPrice() * qty;

        LivestreamGift lg = LivestreamGift.builder()
                .livestream(ls).sender(sender).receiver(ls.getHost())
                .gift(gift).quantity(qty).totalCoinPrice(total)
                .build();
        giftRepo.save(lg);

        ls.setGiftCount(ls.getGiftCount() + qty);
        livestreamRepo.save(ls);

        // Publish gift event
        java.util.Map<String, Object> giftEvent = new java.util.HashMap<>();
        giftEvent.put("senderUsername", sender.getUsername());
        giftEvent.put("senderDisplayName", resolveDisplayName(sender));
        giftEvent.put("senderAvatarUrl", sender.getAvatarUrl());
        giftEvent.put("giftName", gift.getName());
        giftEvent.put("giftIconUrl", gift.getIconUrl());
        giftEvent.put("quantity", qty);
        eventPublisher.publishGift(livestreamId, giftEvent);
    }

    @Override
    public List<GiftCatalogResponse> getGiftCatalog() {
        return giftCatalogRepo.findByIsActiveTrue().stream()
                .map(g -> GiftCatalogResponse.builder()
                        .id(g.getId()).name(g.getName())
                        .iconUrl(g.getIconUrl()).animationUrl(g.getAnimationUrl())
                        .coinPrice(g.getCoinPrice()).build())
                .collect(Collectors.toList());
    }

    // ─── Moderation ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void hideChatMessage(Long livestreamId, Long messageId) {
        User mod = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        if (!isHostOrModerator(ls, mod) && !isAdmin(mod)) throw new AppException(ErrorCode.FORBIDDEN);

        LivestreamChatMessage msg = chatRepo.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        msg.setHidden(true);
        chatRepo.save(msg);

        logModeration(ls, mod, null, messageId, "MESSAGE_HIDDEN", null);
        eventPublisher.publishModerationEvent(livestreamId, "MESSAGE_HIDDEN", messageId, null);
    }

    @Override
    @Transactional
    public void pinChatMessage(Long livestreamId, Long messageId) {
        User mod = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        if (!isHostOrModerator(ls, mod) && !isAdmin(mod)) throw new AppException(ErrorCode.FORBIDDEN);

        // Unpin all first
        chatRepo.findPinnedMessages(ls).forEach(m -> { m.setPinned(false); chatRepo.save(m); });

        LivestreamChatMessage msg = chatRepo.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        msg.setPinned(true);
        chatRepo.save(msg);
    }

    @Override
    @Transactional
    public void banUser(Long livestreamId, Long userId, String reason) {
        User mod = requireCurrentUser();
        Livestream ls = requireLivestream(livestreamId);
        if (!isHostOrModerator(ls, mod) && !isAdmin(mod)) throw new AppException(ErrorCode.FORBIDDEN);

        User target = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!banRepo.existsByLivestreamAndUser(ls, target)) {
            banRepo.save(LivestreamBan.builder()
                    .livestream(ls).user(target).bannedBy(mod).reason(reason).build());
        }

        logModeration(ls, mod, target, null, "USER_BANNED", reason);
        eventPublisher.publishModerationEvent(livestreamId, "USER_BANNED", null, userId);
    }

    private void logModeration(Livestream ls, User mod, User targetUser, Long targetMessageId, String action, String reason) {
        modLogRepo.save(LivestreamModerationLog.builder()
                .livestream(ls).moderator(mod)
                .targetUser(targetUser).targetMessageId(targetMessageId)
                .action(action).reason(reason)
                .build());
    }
}
