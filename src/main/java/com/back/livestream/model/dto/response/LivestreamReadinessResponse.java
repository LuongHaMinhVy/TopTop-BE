package com.back.livestream.model.dto.response;

import com.back.livestream.model.enums.LivestreamStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Lightweight response for the GET /lives/{id}/status endpoint.
 * Used by the frontend to poll for stream readiness without loading full metadata.
 */
@Data
@Builder
public class LivestreamReadinessResponse {
    private Long id;
    private LivestreamStatus status;
    private String roomName;
}
