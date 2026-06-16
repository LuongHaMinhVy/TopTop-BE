package com.back.livestream.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.common.utils.Translator;
import com.back.livestream.model.dto.request.CreateLivestreamRequest;
import com.back.livestream.model.dto.request.SendChatMessageRequest;
import com.back.livestream.model.dto.request.SendGiftRequest;
import com.back.livestream.model.dto.response.*;
import com.back.livestream.service.ILivestreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lives")
@RequiredArgsConstructor
public class LivestreamController {

    private final ILivestreamService livestreamService;

    // ── Feed ──────────────────────────────────────────────────────────────────

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<LivestreamResponse>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<List<LivestreamResponse>>builder()
                .message("Live feed fetched")
                .data(livestreamService.getLiveFeed(page, size))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LivestreamResponse>> getLivestream(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<LivestreamResponse>builder()
                .message("Livestream fetched")
                .data(livestreamService.getLivestream(id))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Lightweight status check used by the frontend polling loop.
     * Returns only id, status, and roomName — no auth required, no heavy joins.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<com.back.livestream.model.dto.response.LivestreamReadinessResponse>> getStreamStatus(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<com.back.livestream.model.dto.response.LivestreamReadinessResponse>builder()
                .message("Stream status fetched")
                .data(livestreamService.getStreamReadiness(id))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<LivestreamResponse>>> getMyLivestreams() {
        return ResponseEntity.ok(ApiResponse.<List<LivestreamResponse>>builder()
                .message("My livestreams fetched")
                .data(livestreamService.getMyLivestreams())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<LivestreamResponse>> createLivestream(
            @Valid @RequestBody CreateLivestreamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<LivestreamResponse>builder()
                        .message("Livestream created")
                        .data(livestreamService.createLivestream(request))
                        .status(HttpStatus.CREATED.value())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<JoinLivestreamResponse>> startLivestream(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<JoinLivestreamResponse>builder()
                .message("Livestream started")
                .data(livestreamService.startLivestream(id))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<JoinLivestreamResponse>> joinLivestream(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<JoinLivestreamResponse>builder()
                .message("Joined livestream")
                .data(livestreamService.joinLivestream(id))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<Void>> endLivestream(@PathVariable Long id) {
        livestreamService.endLivestream(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Livestream ended")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveLivestream(@PathVariable Long id) {
        livestreamService.leaveStream(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Left livestream")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/chat/messages")
    public ResponseEntity<ApiResponse<List<LiveChatMessageResponse>>> getChatHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.<List<LiveChatMessageResponse>>builder()
                .message("Chat history fetched")
                .data(livestreamService.getChatHistory(id, page, size))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/chat/messages")
    public ResponseEntity<ApiResponse<LiveChatMessageResponse>> sendChatMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.<LiveChatMessageResponse>builder()
                .message("Message sent")
                .data(livestreamService.sendChatMessage(id, request))
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<Void>> sendReaction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "LIKE");
        livestreamService.sendReaction(id, type);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Reaction sent")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Gifts ─────────────────────────────────────────────────────────────────

    @GetMapping("/gifts/catalog")
    public ResponseEntity<ApiResponse<List<GiftCatalogResponse>>> getGiftCatalog() {
        return ResponseEntity.ok(ApiResponse.<List<GiftCatalogResponse>>builder()
                .message("Gift catalog fetched")
                .data(livestreamService.getGiftCatalog())
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/gifts")
    public ResponseEntity<ApiResponse<Void>> sendGift(
            @PathVariable Long id,
            @Valid @RequestBody SendGiftRequest request) {
        livestreamService.sendGift(id, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Gift sent")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Moderation ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/moderation/messages/{messageId}/hide")
    public ResponseEntity<ApiResponse<Void>> hideMessage(
            @PathVariable Long id, @PathVariable Long messageId) {
        livestreamService.hideChatMessage(id, messageId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Message hidden")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/moderation/messages/{messageId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinMessage(
            @PathVariable Long id, @PathVariable Long messageId) {
        livestreamService.pinChatMessage(id, messageId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Message pinned")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/{id}/moderation/users/{userId}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable Long id, @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        livestreamService.banUser(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("User banned")
                .status(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
