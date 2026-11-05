package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "PRODUCT", schema = "dbo")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 80)
    private String barcode;

    @Column(name = "product_name", nullable = false, length = 180)
    private String name;

    @Column(name = "cost_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 30)
    private String unit = "cái";

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 5;

    @jakarta.persistence.Transient
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    protected Product() {
    }

    public static Product create(Category category, String name, String barcode,
                                 BigDecimal price, String imageUrl) {
        Product p = new Product();
        p.category = category;
        p.name = name;
        p.barcode = (barcode != null && !barcode.isBlank()) ? barcode.trim() : null;
        p.sku = "SKU-" + System.currentTimeMillis();
        p.costPrice = price.multiply(new BigDecimal("0.7"));
        p.price = price;
        p.unit = "cái";
        p.reorderLevel = 5;
        p.imageUrl = imageUrl;
        p.status = ProductStatus.ACTIVE;
        return p;
    }

    public void updateDetails(Category category, String name, String barcode,
                              BigDecimal price, String imageUrl, ProductStatus status) {
        this.category = category;
        this.name = name;
        this.barcode = (barcode != null && !barcode.isBlank()) ? barcode.trim() : null;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public Long getSupplierId() { return supplierId; }
    public String getSku() { return sku; }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getPrice() { return price; }
    public String getUnit() { return unit; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public String getImageUrl() { return imageUrl; }
    public ProductStatus getStatus() { return status; }
}