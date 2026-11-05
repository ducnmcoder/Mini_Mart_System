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

import java.math.BigDecimal;

@Entity
@Table(name = "SALE_ITEM", schema = "dbo")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    protected SaleItem() {
    }

    public static SaleItem of(Sale sale, Product product, StorageLocation location, int quantity) {
        SaleItem item = new SaleItem();
        item.sale = sale;
        item.product = product;
        item.location = location;
        item.quantity = quantity;
        item.unitPrice = product.getPrice();
        item.discountAmount = BigDecimal.ZERO;
        item.lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public Product getProduct() { return product; }
    public StorageLocation getLocation() { return location; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
}