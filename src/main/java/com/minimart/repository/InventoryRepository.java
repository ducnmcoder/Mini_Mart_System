package com.minimart.repository;
import com.minimart.validation.InventoryValidationException;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class InventoryRepository {
    private final JdbcTemplate jdbc;
    public InventoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> locations() {
        return jdbc.queryForList("SELECT * FROM dbo.STORAGE_LOCATION WHERE is_active=1 ORDER BY location_code");
    }
    public List<Map<String, Object>> stock() {
        return jdbc.queryForList("""
            SELECT i.product_id, i.location_id, p.sku, p.product_name, p.unit,
                   l.location_code, l.location_name, i.quantity_on_hand, i.quantity_reserved,
                   i.quantity_on_hand-i.quantity_reserved AS available
            FROM dbo.INVENTORY i JOIN dbo.PRODUCT p ON p.product_id=i.product_id
            JOIN dbo.STORAGE_LOCATION l ON l.location_id=i.location_id
            ORDER BY p.product_name, l.location_code
            """);
    }
    public List<Map<String, Object>> history() {
        return jdbc.queryForList("""
            SELECT TOP (50) a.adjustment_no, a.reason, a.adjusted_at, u.full_name,
                   p.sku, l.location_code, t.quantity_change
            FROM dbo.STOCK_ADJUSTMENT a
            JOIN dbo.STOCK_ADJUSTMENT_ITEM t ON t.adjustment_id=a.adjustment_id
            JOIN dbo.PRODUCT p ON p.product_id=t.product_id
            JOIN dbo.STORAGE_LOCATION l ON l.location_id=t.location_id
            JOIN dbo.USER_ACCOUNT u ON u.account_id=a.adjusted_by
            WHERE a.adjustment_no LIKE 'MOVE-%' AND a.status='APPLIED'
            ORDER BY a.adjustment_id DESC, t.quantity_change
            """);
    }
    // Lock locations in a stable order, shared by transfers, edits and deletion.
    public void lockLocation(long id) {
        var rows = jdbc.queryForList("SELECT location_id FROM dbo.STORAGE_LOCATION WITH (UPDLOCK,HOLDLOCK) WHERE location_id=? AND is_active=1", id);
        if (rows.isEmpty()) throw new InventoryValidationException("Gian hàng không tồn tại hoặc đã bị xóa.");
    }
    public long createLocation(String code, String name, String description) {
        return jdbc.queryForObject("INSERT dbo.STORAGE_LOCATION(location_code,location_name,description) OUTPUT INSERTED.location_id VALUES (?,?,?)", Long.class, code, name, description);
    }
    public void updateLocation(long id, String code, String name, String description) {
        jdbc.update("UPDATE dbo.STORAGE_LOCATION SET location_code=?,location_name=?,description=? WHERE location_id=?", code,name,description,id);
    }
    public boolean hasStock(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.INVENTORY WITH (UPDLOCK,HOLDLOCK) WHERE location_id=? AND (quantity_on_hand>0 OR quantity_reserved>0)", Long.class,id)>0;
    }
    public void deactivate(long id) { jdbc.update("UPDATE dbo.STORAGE_LOCATION SET is_active=0 WHERE location_id=?",id); }
    public void transfer(long product, long source, long target, int quantity, long actor, String reason, String number) {
        if (jdbc.queryForList("SELECT product_id FROM dbo.PRODUCT WITH (HOLDLOCK) WHERE product_id=? AND status='ACTIVE'",product).isEmpty())
            throw new InventoryValidationException("Sản phẩm không tồn tại hoặc đã ngừng hoạt động.");
        int changed = jdbc.update("""
            UPDATE dbo.INVENTORY WITH (UPDLOCK,HOLDLOCK)
            SET quantity_on_hand=quantity_on_hand-?,updated_at=SYSUTCDATETIME()
            WHERE product_id=? AND location_id=? AND quantity_on_hand-quantity_reserved>=?
            """,quantity,product,source,quantity);
        if (changed!=1) throw new InventoryValidationException("Số lượng chuyển vượt tồn khả dụng tại gian hàng nguồn.");
        if (jdbc.queryForList("SELECT inventory_id FROM dbo.INVENTORY WITH (UPDLOCK,HOLDLOCK) WHERE product_id=? AND location_id=?", product,target).isEmpty())
            jdbc.update("INSERT dbo.INVENTORY(product_id,location_id,quantity_on_hand,quantity_reserved) VALUES (?,?,0,0)",product,target);
        long balance = jdbc.queryForObject("SELECT CAST(quantity_on_hand AS BIGINT) FROM dbo.INVENTORY WHERE product_id=? AND location_id=?",Long.class,product,target);
        if (balance+quantity>Integer.MAX_VALUE) throw new InventoryValidationException("Tồn tại gian hàng đích vượt giới hạn cho phép.");
        jdbc.update("UPDATE dbo.INVENTORY SET quantity_on_hand=quantity_on_hand+?,updated_at=SYSUTCDATETIME() WHERE product_id=? AND location_id=?",quantity,product,target);
        Long document = jdbc.queryForObject("INSERT dbo.STOCK_ADJUSTMENT(adjustment_no,adjusted_by,reason,status) OUTPUT INSERTED.adjustment_id VALUES (?,?,?,'APPLIED')",Long.class,number,actor,reason);
        for (long location : new long[]{source,target}) {
            int delta = location==source ? -quantity : quantity;
            jdbc.update("INSERT dbo.STOCK_ADJUSTMENT_ITEM(adjustment_id,product_id,location_id,quantity_change,note) VALUES (?,?,?,?,?)",document,product,location,delta,"Chuyển giữa các gian hàng");
            jdbc.update("INSERT dbo.STOCK_TRANSACTION(product_id,location_id,performed_by,transaction_type,quantity_change,reference_type,reference_id) VALUES (?,?,?,'ADJUSTMENT',?,'STOCK_ADJUSTMENT',?)",product,location,actor,delta,document);
        }
    }
}
