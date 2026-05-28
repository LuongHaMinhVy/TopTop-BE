package com.back.admin.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardStatsResponseDTO {
    private long totalUsers;
    private long totalVideos;
    private long pendingModerationVideos;
    private long totalReports;
}
