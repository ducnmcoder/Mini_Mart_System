package com.minimart.service;

import com.minimart.dto.response.DashboardMetricDto;
import com.minimart.entity.AppRole;

import java.util.List;

public interface DashboardService {

    List<DashboardMetricDto> getMetrics(AppRole role);
}
