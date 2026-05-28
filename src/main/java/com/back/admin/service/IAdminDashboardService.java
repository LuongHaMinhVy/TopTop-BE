package com.back.admin.service;

import com.back.admin.model.dto.response.AdminDashboardStatsResponseDTO;

public interface IAdminDashboardService {
    AdminDashboardStatsResponseDTO getDashboardStats();
}
