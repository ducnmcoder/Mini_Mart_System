package com.minimart.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleReceiptDto(
        Long saleId,
        String saleNo,
        String cashierName,
        String paymentMethod,
        BigDecimal cashGiven,
        BigDecimal changeDue,
        List<SaleReceiptItemDto> items,
        BigDecimal grandTotal,
        LocalDateTime createdAt
) {
}