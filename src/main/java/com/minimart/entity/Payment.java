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
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT", schema = "dbo")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(nullable = false, length = 20)
    private String method; // 'CASH' or 'VNPAY'

    @Column(length = 30)
    private String provider;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 25)
    private String status; // 'PENDING', 'PAID', 'FAILED', 'CANCELLED'

    @Column(name = "transaction_ref", length = 120)
    private String transactionRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected Payment() {
    }

    public static Payment create(Sale sale, String method, BigDecimal amount, String transactionRef) {
        Payment p = new Payment();
        p.sale           = sale;
        p.method         = method;
        p.provider       = "VNPAY".equalsIgnoreCase(method) ? "VNPAY" : null;
        p.amount         = amount;
        p.status         = "PAID";
        p.transactionRef = transactionRef;
        p.paidAt         = LocalDateTime.now();
        return p;
    }

    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public String getMethod() { return method; }
    public String getProvider() { return provider; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public LocalDateTime getPaidAt() { return paidAt; }
}