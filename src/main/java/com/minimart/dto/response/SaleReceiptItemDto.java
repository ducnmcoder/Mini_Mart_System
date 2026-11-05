package com.minimart.dto.response;

import java.math.BigDecimal;

public record SaleReceiptItemDto(
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
