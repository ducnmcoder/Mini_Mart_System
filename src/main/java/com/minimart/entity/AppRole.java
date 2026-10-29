package com.minimart.entity;

public enum AppRole {
    ADMIN("ADMIN DASHBOARD", "Quản trị hệ thống", "/dashboard/admin"),
    OWNER("OWNER DASHBOARD", "Chủ cửa hàng", "/dashboard/owner"),
    SALE_STAFF("SALE STAFF DASHBOARD", "Nhân viên bán hàng", "/dashboard/sale-staff"),
    INVENTORY_STAFF("INVENTORY STAFF DASHBOARD", "Nhân viên kho", "/dashboard/inventory-staff");

    private final String dashboardTitle;
    private final String vietnameseName;
    private final String dashboardPath;

    AppRole(String dashboardTitle, String vietnameseName, String dashboardPath) {
        this.dashboardTitle = dashboardTitle;
        this.vietnameseName = vietnameseName;
        this.dashboardPath = dashboardPath;
    }

    public String getDashboardTitle() {
        return dashboardTitle;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public String dashboardPath() {
        return dashboardPath;
    }
}
