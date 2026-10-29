package com.minimart.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppRoleTest {

    @Test
    void createsDedicatedPathForEveryRole() {
        assertThat(AppRole.ADMIN.dashboardPath()).isEqualTo("/dashboard/admin");
        assertThat(AppRole.OWNER.dashboardPath()).isEqualTo("/dashboard/owner");
        assertThat(AppRole.SALE_STAFF.dashboardPath()).isEqualTo("/dashboard/sale-staff");
        assertThat(AppRole.INVENTORY_STAFF.dashboardPath()).isEqualTo("/dashboard/inventory-staff");
    }
}
