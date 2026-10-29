package com.minimart.entity.converter;

import com.minimart.entity.AppRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppRoleConverterTest {

    private final AppRoleConverter converter = new AppRoleConverter();

    @Test
    void mapsLegacyWarehouseRoleToInventoryStaff() {
        assertThat(converter.convertToEntityAttribute("WAREHOUSE_STAFF"))
                .isEqualTo(AppRole.INVENTORY_STAFF);
    }

    @Test
    void mapsCurrentInventoryRole() {
        assertThat(converter.convertToEntityAttribute("INVENTORY_STAFF"))
                .isEqualTo(AppRole.INVENTORY_STAFF);
    }
}
