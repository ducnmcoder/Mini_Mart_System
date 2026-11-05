package com.minimart.dto.response;

import java.math.BigDecimal;

public record ProductSearchResultDto(
        Long id,
        String name,
        String barcode,
        BigDecimal price,
        int quantityOnHand,
        String imageUrl,
        String categoryName
) {
}
