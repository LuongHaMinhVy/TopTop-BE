package com.back.admin.service.impl;

import com.back.admin.model.dto.response.AdminDashboardStatsResponseDTO;
import com.back.admin.service.IAdminDashboardService;
import com.back.moderation.model.enums.VideoModerationStatus;
import com.back.report.repo.IReportRepo;
import com.back.user.repo.IUserRepo;
import com.back.video.repo.IVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final IUserRepo userRepo;
    private final IVideoRepository videoRepo;
    private final IReportRepo reportRepo;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponseDTO getDashboardStats() {
        long totalUsers = userRepo.count();
        long totalVideos = videoRepo.count();
        long pendingModeration = videoRepo.countByModerationStatus(VideoModerationStatus.NEED_REVIEW);
        long totalReports = reportRepo.countByStatus(com.back.report.model.enums.ReportStatus.PENDING);

        return AdminDashboardStatsResponseDTO.builder()
                .totalUsers(totalUsers)
                .totalVideos(totalVideos)
                .pendingModerationVideos(pendingModeration)
                .totalReports(totalReports)
                .build();
    }
}
