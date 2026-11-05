package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOW_STOCK_ALERT", schema = "dbo")
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(name = "current_qty", nullable = false)
    private int currentQty;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected LowStockAlert() {
    }

    public static LowStockAlert create(Product product, StorageLocation location, int currentQty) {
        LowStockAlert alert = new LowStockAlert();
        alert.product = product;
        alert.location = location;
        alert.currentQty = currentQty;
        alert.reorderLevel = product.getReorderLevel();
        alert.status = "ACTIVE";
        return alert;
    }

    public void resolve() {
        this.status = "RESOLVED";
        this.resolvedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public StorageLocation getLocation() { return location; }
    public int getCurrentQty() { return currentQty; }
    public int getReorderLevel() { return reorderLevel; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}