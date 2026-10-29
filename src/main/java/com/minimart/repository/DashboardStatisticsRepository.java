package com.minimart.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class DashboardStatisticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardStatisticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countAccounts() {
        return count("SELECT COUNT(*) FROM dbo.USER_ACCOUNT");
    }

    public long countActiveAccounts() {
        return count("SELECT COUNT(*) FROM dbo.USER_ACCOUNT WHERE status='ACTIVE'");
    }

    public long countActiveRoles() {
        return count("SELECT COUNT(*) FROM dbo.ROLE WHERE is_active=1");
    }

    public BigDecimal completedRevenue() {
        return money("SELECT COALESCE(SUM(grand_total),0) FROM dbo.SALE WHERE status='COMPLETED'");
    }

    public long countCompletedSales() {
        return count("SELECT COUNT(*) FROM dbo.SALE WHERE status='COMPLETED'");
    }

    public long countActiveLowStockAlerts() {
        return count("SELECT COUNT(*) FROM dbo.LOW_STOCK_ALERT WHERE status='ACTIVE'");
    }

    public long countActiveProducts() {
        return count("SELECT COUNT(*) FROM dbo.PRODUCT WHERE status='ACTIVE'");
    }

    public long totalQuantityOnHand() {
        return count("SELECT COALESCE(SUM(quantity_on_hand),0) FROM dbo.INVENTORY");
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private BigDecimal money(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }
}
