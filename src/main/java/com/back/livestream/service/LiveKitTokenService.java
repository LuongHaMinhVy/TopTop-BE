package com.back.livestream.service;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomAdmin;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class LiveKitTokenService {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Getter
    @Value("${livekit.url}")
    private String livekitUrl;

    @PostConstruct
    void validateConfig() {
        if (isBlank(livekitUrl) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException("LiveKit config is incomplete. Check LIVEKIT_URL, LIVEKIT_API_KEY, and LIVEKIT_API_SECRET.");
        }
        if (!livekitUrl.startsWith("wss://") && !livekitUrl.startsWith("ws://")) {
            throw new IllegalStateException("LIVEKIT_URL must be a websocket URL, for example wss://your-project.livekit.cloud.");
        }
        log.info("LiveKit configured for {}", livekitUrl);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AccessToken createToken(String roomName, String appUserId, String displayName, String role) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(displayName);
        token.setIdentity(buildLiveKitIdentity(appUserId, role));
        token.getAttributes().put("appUserId", appUserId);
        token.getAttributes().put("role", role);
        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName)
        );
        return token;
    }

    private String buildLiveKitIdentity(String appUserId, String role) {
        return "user_" + appUserId + "_" + role.toLowerCase(Locale.ROOT) + "_" + UUID.randomUUID();
    }

    public String generateHostToken(String roomName, String identity, String displayName) {
        AccessToken token = createToken(roomName, identity, displayName, "HOST");
        token.addGrants(
                new CanPublish(true),
                new CanSubscribe(true),
                new CanPublishData(true)
        );
        return token.toJwt();
    }

    public String generateViewerToken(String roomName, String identity, String displayName) {
        AccessToken token = createToken(roomName, identity, displayName, "VIEWER");
        token.addGrants(
                new CanPublish(false),
                new CanSubscribe(true),
                new CanPublishData(true)
        );
        return token.toJwt();
    }

    public String generateModeratorToken(String roomName, String identity, String displayName) {
        AccessToken token = createToken(roomName, identity, displayName, "MODERATOR");
        token.addGrants(
                new CanPublish(false),
                new CanSubscribe(true),
                new CanPublishData(true),
                new RoomAdmin(true)
        );
        return token.toJwt();
    }
}
