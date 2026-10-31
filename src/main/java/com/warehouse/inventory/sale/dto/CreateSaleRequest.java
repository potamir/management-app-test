package com.warehouse.inventory.sale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Records a sale and decrements stock atomically.
 *
 * <p>{@code variantId} is required when the item has variants (stock is then
 * tracked per variant) and must be null/omitted otherwise.</p>
 */
public record CreateSaleRequest(

        @NotNull(message = "itemId is required")
        Long itemId,

        Long variantId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
