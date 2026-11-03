package com.minimart.service;
import com.minimart.validation.InventoryValidationException;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt in with -Dminimart.sql.integration=true against a local test database. All writes roll back. */
@SpringBootTest(properties="app.bootstrap-demo.enabled=false")
@AutoConfigureMockMvc
@Transactional
@EnabledIfSystemProperty(named="minimart.sql.integration", matches="true")
class InventorySqlIntegrationTest {
    @Autowired InventoryService inventory;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test void transferConservesStockAndRecordsBothSides() {
        long actor=jdbc.queryForObject("SELECT TOP 1 account_id FROM dbo.USER_ACCOUNT ORDER BY account_id",Long.class);
        long product=jdbc.queryForObject("SELECT TOP 1 product_id FROM dbo.PRODUCT WHERE status='ACTIVE' ORDER BY product_id",Long.class);
        String token=Long.toString(System.nanoTime());
        inventory.saveLocation(null,"TEST-A"+token,"Nguồn kiểm thử","",actor);
        inventory.saveLocation(null,"TEST-B"+token,"Đích kiểm thử","",actor);
        long source=jdbc.queryForObject("SELECT location_id FROM dbo.STORAGE_LOCATION WHERE location_code=?",Long.class,"TEST-A"+token);
        long target=jdbc.queryForObject("SELECT location_id FROM dbo.STORAGE_LOCATION WHERE location_code=?",Long.class,"TEST-B"+token);
        jdbc.update("INSERT dbo.INVENTORY(product_id,location_id,quantity_on_hand,quantity_reserved) VALUES (?,?,10,2)",product,source);
        inventory.transfer(product,source,target,5,"Kiểm thử chuyển hàng",actor);
        assertEquals(5,jdbc.queryForObject("SELECT quantity_on_hand FROM dbo.INVENTORY WHERE product_id=? AND location_id=?",Integer.class,product,source));
        assertEquals(5,jdbc.queryForObject("SELECT quantity_on_hand FROM dbo.INVENTORY WHERE product_id=? AND location_id=?",Integer.class,product,target));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.STOCK_TRANSACTION WHERE product_id=? AND location_id IN (?,?)",Integer.class,product,source,target));
        assertEquals(0,jdbc.queryForObject("SELECT SUM(quantity_change) FROM dbo.STOCK_TRANSACTION WHERE product_id=? AND location_id IN (?,?)",Integer.class,product,source,target));
        assertThrows(InventoryValidationException.class,()->inventory.transfer(product,source,target,4,"Vượt tồn khả dụng",actor));
        assertEquals(5,jdbc.queryForObject("SELECT quantity_on_hand FROM dbo.INVENTORY WHERE product_id=? AND location_id=?",Integer.class,product,source));
        assertThrows(InventoryValidationException.class,()->inventory.deleteLocation(source,actor));
    }

    @Test void ownerCanEditAndDeleteAnEmptyLocation() {
        long actor=jdbc.queryForObject("SELECT TOP 1 account_id FROM dbo.USER_ACCOUNT ORDER BY account_id",Long.class);
        String code="TEST-"+System.nanoTime();
        inventory.saveLocation(null,code,"Gian hàng mới","",actor);
        long id=jdbc.queryForObject("SELECT location_id FROM dbo.STORAGE_LOCATION WHERE location_code=?",Long.class,code);
        inventory.saveLocation(id,code,"Gian hàng đã sửa","Mô tả",actor);
        assertEquals("Gian hàng đã sửa",jdbc.queryForObject("SELECT location_name FROM dbo.STORAGE_LOCATION WHERE location_id=?",String.class,id));
        inventory.deleteLocation(id,actor);
        assertEquals(false,jdbc.queryForObject("SELECT is_active FROM dbo.STORAGE_LOCATION WHERE location_id=?",Boolean.class,id));
    }

    @Test void dashboardsRenderAndOtherRolesCannotChangeInventory() throws Exception {
        String owner=jdbc.queryForObject("SELECT TOP 1 u.email FROM dbo.USER_ACCOUNT u JOIN dbo.ROLE r ON r.role_id=u.role_id WHERE r.role_name='OWNER'",String.class);
        String staff=jdbc.queryForObject("SELECT TOP 1 u.email FROM dbo.USER_ACCOUNT u JOIN dbo.ROLE r ON r.role_id=u.role_id WHERE r.role_name='WAREHOUSE_STAFF'",String.class);
        mvc.perform(get("/dashboard/owner").with(user(owner).roles("OWNER"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý gian hàng")));
        mvc.perform(get("/dashboard/inventory-staff").with(user(staff).roles("INVENTORY_STAFF"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Phân bổ hàng vào gian hàng")));
        mvc.perform(post("/inventory/transfers").with(user(owner).roles("OWNER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/owner/locations/save").with(user(staff).roles("INVENTORY_STAFF")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/inventory/transfers").with(user(staff).roles("INVENTORY_STAFF"))).andExpect(status().isForbidden());
    }
}
