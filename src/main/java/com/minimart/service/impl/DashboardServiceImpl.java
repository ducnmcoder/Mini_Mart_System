package com.minimart.service.impl;

import com.minimart.dto.response.DashboardMetricDto;
import com.minimart.entity.AppRole;
import com.minimart.repository.DashboardStatisticsRepository;
import com.minimart.service.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private final DashboardStatisticsRepository statisticsRepository;

    public DashboardServiceImpl(DashboardStatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @Override
    public List<DashboardMetricDto> getMetrics(AppRole role) {
        return switch (role) {
            case ADMIN -> adminMetrics();
            case OWNER -> ownerMetrics();
            case SALE_STAFF -> List.of();
            case INVENTORY_STAFF -> List.of();
        };
    }

    private List<DashboardMetricDto> adminMetrics() {
        return List.of(
                metric("Tổng tài khoản", statisticsRepository.countAccounts(), "Tất cả người dùng", "users"),
                metric("Đang hoạt động", statisticsRepository.countActiveAccounts(), "Tài khoản đã kích hoạt", "check"),
                metric("Nhóm quyền", statisticsRepository.countActiveRoles(), "Vai trò đang sử dụng", "shield")
        );
    }

    private List<DashboardMetricDto> ownerMetrics() {
        long lowStockAlerts = statisticsRepository.countActiveLowStockAlerts();
        return List.of(
                moneyMetric("Doanh thu", statisticsRepository.completedRevenue(), "Đơn hàng hoàn tất", "revenue"),
                metric("Đơn hàng", statisticsRepository.countCompletedSales(), "Tổng giao dịch thành công", "receipt"),
                metric("Cảnh báo tồn", lowStockAlerts, "Sản phẩm cần nhập thêm", "alert"),
                metric("Sản phẩm", statisticsRepository.countActiveProducts(), "Đang kinh doanh", "box"),
                metric("Tồn kho", statisticsRepository.totalQuantityOnHand(), "Tổng số lượng hiện có", "warehouse"),
                metric("Sắp hết hàng", lowStockAlerts, "Cảnh báo đang mở", "alert")
        );
    }

    private DashboardMetricDto metric(String label, long value, String note, String icon) {
        return new DashboardMetricDto(label, NumberFormat.getIntegerInstance(VIETNAM).format(value), note, icon);
    }

    private DashboardMetricDto moneyMetric(String label, BigDecimal value, String note, String icon) {
        return new DashboardMetricDto(label, NumberFormat.getCurrencyInstance(VIETNAM).format(value), note, icon);
    }
}
