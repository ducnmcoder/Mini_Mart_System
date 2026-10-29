package com.minimart.service.impl;

import com.minimart.entity.AppRole;
import com.minimart.repository.DashboardStatisticsRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceImplTest {

    @Test
    void saleStaffDoesNotReceiveDashboardMetrics() {
        DashboardServiceImpl dashboardService = new DashboardServiceImpl(null);

        assertThat(dashboardService.getMetrics(AppRole.SALE_STAFF)).isEmpty();
    }

    @Test
    void inventoryStaffDoesNotReceiveDashboardMetrics() {
        DashboardServiceImpl dashboardService = new DashboardServiceImpl(null);

        assertThat(dashboardService.getMetrics(AppRole.INVENTORY_STAFF)).isEmpty();
    }

    @Test
    void ownerReceivesOwnerAndInventoryMetrics() {
        DashboardStatisticsRepository statisticsRepository = new DashboardStatisticsRepository(null) {
            @Override
            public BigDecimal completedRevenue() {
                return BigDecimal.valueOf(71000);
            }

            @Override
            public long countCompletedSales() {
                return 1;
            }

            @Override
            public long countActiveLowStockAlerts() {
                return 1;
            }

            @Override
            public long countActiveProducts() {
                return 5;
            }

            @Override
            public long totalQuantityOnHand() {
                return 236;
            }
        };
        DashboardServiceImpl dashboardService = new DashboardServiceImpl(statisticsRepository);

        assertThat(dashboardService.getMetrics(AppRole.OWNER))
                .extracting("label")
                .containsExactly("Doanh thu", "Đơn hàng", "Cảnh báo tồn", "Sản phẩm", "Tồn kho", "Sắp hết hàng");
    }
}
