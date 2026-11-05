package com.minimart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public class CartCheckoutRequest {

    @NotEmpty(message = "Giỏ hàng không được để trống.")
    @Valid
    private List<CartItemRequest> items;

    private String paymentMethod = "CASH"; // "CASH" hoặc "VNPAY"

    private BigDecimal cashGiven; // Số tiền khách đưa khi trả tiền mặt

    private String vnpayTxnRef; // Mã giao dịch VNPay nếu thanh toán qua QR

    public List<CartItemRequest> getItems() { return items; }
    public void setItems(List<CartItemRequest> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getCashGiven() { return cashGiven; }
    public void setCashGiven(BigDecimal cashGiven) { this.cashGiven = cashGiven; }
    public String getVnpayTxnRef() { return vnpayTxnRef; }
    public void setVnpayTxnRef(String vnpayTxnRef) { this.vnpayTxnRef = vnpayTxnRef; }
}