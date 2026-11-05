package com.minimart.dto.response;

import com.minimart.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductDetailDto(
        Long id,
        String name,
        String barcode,
        BigDecimal price,
        String imageUrl,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        int quantityOnHand,
        int lowStockThreshold,
        String sku
) {
}