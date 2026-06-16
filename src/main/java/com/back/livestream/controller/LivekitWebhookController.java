package com.back.livestream.controller;

import com.back.livestream.service.ILivestreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives LiveKit server-sent webhook events (participant join/leave, room finished).
 * Delegate all parsing, validation, and database updates to the service layer.
 */
@RestController
@RequestMapping("/api/v1/lives/livekit")
@RequiredArgsConstructor
@Slf4j
public class LivekitWebhookController {

    private final ILivestreamService livestreamService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String rawBody) {

        log.debug("LiveKit webhook HTTP POST request received");
        livestreamService.handleLivekitWebhook(authHeader, rawBody);
        return ResponseEntity.ok().build();
    }
}
