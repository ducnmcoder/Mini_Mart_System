package com.minimart.ui.model;

import com.minimart.dto.response.AccountProfileDto;
import com.minimart.dto.response.DashboardMetricDto;

import java.util.List;

public record DashboardPageModel(
        AccountProfileDto account,
        String dashboardTitle,
        String roleName,
        List<DashboardMetricDto> metrics
) {
}
