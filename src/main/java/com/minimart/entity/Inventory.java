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
@Table(name = "INVENTORY", schema = "dbo")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Inventory() {
    }

    public static Inventory createFor(Product product, StorageLocation location, int initialQuantity) {
        Inventory inv = new Inventory();
        inv.product = product;
        inv.location = location;
        inv.quantityOnHand = initialQuantity;
        inv.quantityReserved = 0;
        inv.updatedAt = LocalDateTime.now();
        return inv;
    }

    public void adjustQuantity(int delta) {
        int newQty = this.quantityOnHand + delta;
        if (newQty < 0) {
            throw new IllegalStateException("Số lượng tồn kho không đủ cho sản phẩm: " + product.getName());
        }
        this.quantityOnHand = newQty;
        this.updatedAt = LocalDateTime.now();
    }

    public void setQuantity(int quantity) {
        this.quantityOnHand = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public StorageLocation getLocation() { return location; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public int getQuantityReserved() { return quantityReserved; }
    public LocalDateTime getLastCountedAt() { return lastCountedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}