package com.warehouse.inventory.item.dto;

import com.warehouse.inventory.item.Variant;
import java.math.BigDecimal;

public record VariantResponse(
        Long id,
        Long itemId,
        String name,
        String sku,
        BigDecimal price,
        int stockQuantity,
        boolean inStock
) {

    public static VariantResponse from(Variant variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getItem().getId(),
                variant.getName(),
                variant.getSku(),
                variant.effectivePrice(),
                variant.getStockQuantity(),
                variant.getStockQuantity() > 0);
    }
}
