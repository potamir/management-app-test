package com.warehouse.inventory.sale.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.warehouse.inventory.sale.Sale;
import java.math.BigDecimal;
import java.time.Instant;

public record SaleResponse(
        Long id,
        Long itemId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long variantId,
        String itemName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String variantName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        Instant createdAt,
        int remainingStock
) {

    public static SaleResponse from(Sale sale, int remainingStock) {
        return new SaleResponse(
                sale.getId(),
                sale.getItemId(),
                sale.getVariantId(),
                sale.getItemName(),
                sale.getVariantName(),
                sale.getQuantity(),
                sale.getUnitPrice(),
                sale.getTotalAmount(),
                sale.getCreatedAt(),
                remainingStock);
    }
}
