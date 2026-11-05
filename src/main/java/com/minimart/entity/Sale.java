package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SALE", schema = "dbo")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "sale_no", nullable = false, unique = true, length = 40)
    private String saleNo;

    @Column(nullable = false, length = 25)
    private String status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "grand_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "sold_at", nullable = false)
    private LocalDateTime soldAt;

    protected Sale() {
    }

    public static Sale create(Long cashierId, BigDecimal grandTotal) {
        Sale sale = new Sale();
        sale.cashierId = cashierId;
        sale.saleNo = "SALE-" + System.currentTimeMillis();
        sale.status = "PENDING_PAYMENT";
        sale.subtotal = grandTotal;
        sale.discountTotal = BigDecimal.ZERO;
        sale.grandTotal = grandTotal;
        sale.soldAt = LocalDateTime.now();
        return sale;
    }

    public void complete() {
        this.status = "COMPLETED";
        this.soldAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public Long getId() { return id; }
    public Long getCashierId() { return cashierId; }
    public String getSaleNo() { return saleNo; }
    public String getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public LocalDateTime getSoldAt() { return soldAt; }
}