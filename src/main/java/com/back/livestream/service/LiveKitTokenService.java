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
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class LiveKitTokenService {

    @Getter
    @Value("${livekit.api-key}")
    private String apiKey;

    @Getter
    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Getter
    @Value("${livekit.url}")
    private String livekitUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostConstruct
    void validateConfig() {
        if (isBlank(livekitUrl) || isBlank(apiKey) || isBlank(apiSecret)) {
            log.warn("LiveKit config is incomplete. Check LIVEKIT_URL, LIVEKIT_API_KEY, and LIVEKIT_API_SECRET. Livestreaming features will fail until these are set.");
            return;
        }
        if (!livekitUrl.startsWith("wss://") && !livekitUrl.startsWith("ws://")) {
            log.warn("LIVEKIT_URL must be a websocket URL, for example wss://your-project.livekit.cloud. Currently configured: {}", livekitUrl);
            return;
        }
        log.info("LiveKit configured for {}", livekitUrl);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AccessToken createToken(String roomName, String appUserId, String displayName, String role) {
        if (isBlank(apiKey) || isBlank(apiSecret)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "livekit", "Cannot create LiveKit token because LiveKit credentials are not configured.");
        }
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

    public void deleteRoom(String roomName) {
        if (isBlank(roomName)) {
            return;
        }

        String serviceToken = generateRoomServiceToken();
        String endpoint = toHttpUrl(livekitUrl) + "/twirp/livekit.RoomService/DeleteRoom";
        String body = "{\"room\":\"" + escapeJson(roomName) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + serviceToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                log.info("Deleted LiveKit room {}", roomName);
                return;
            }
            if (status == 404 || response.body().contains("not_found")) {
                log.info("LiveKit room {} was already gone", roomName);
                return;
            }
            throw new AppException(ErrorCode.INTERNAL_ERROR, "livekit", "LiveKit DeleteRoom failed with HTTP " + status + ": " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.INTERNAL_ERROR, "livekit", "Interrupted while deleting LiveKit room " + roomName);
        } catch (Exception e) {
            log.warn("Could not delete LiveKit room {}: {}", roomName, e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "livekit", "Could not delete LiveKit room " + roomName);
        }
    }

    private String generateRoomServiceToken() {
        if (isBlank(apiKey) || isBlank(apiSecret)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "livekit", "Cannot generate LiveKit service token because LiveKit credentials are not configured.");
        }
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity("server_livestream_service");
        token.setName("Livestream Server");
        token.addGrants(
                new RoomAdmin(true)
        );
        return token.toJwt();
    }

    private String toHttpUrl(String url) {
        String normalized = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        if (normalized.startsWith("wss://")) {
            return "https://" + normalized.substring("wss://".length());
        }
        if (normalized.startsWith("ws://")) {
            return "http://" + normalized.substring("ws://".length());
        }
        return normalized;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
