package com.warehouse.inventory.item.dto;

import com.warehouse.inventory.item.Item;
import java.math.BigDecimal;
import java.util.List;

public record ItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean inStock,
        List<VariantResponse> variants
) {

    /**
     * Builds the API view of an item. When the item has variants, stock is
     * tracked per variant, so the item's quantity is the sum of its variants.
     */
    public static ItemResponse from(Item item) {
        List<VariantResponse> variants = item.getVariants().stream()
                .map(VariantResponse::from)
                .toList();
        int stock = variants.isEmpty()
                ? item.getStockQuantity()
                : item.getVariants().stream().mapToInt(v -> v.getStockQuantity()).sum();
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                stock,
                stock > 0,
                variants);
    }
}
