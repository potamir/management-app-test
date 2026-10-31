package com.warehouse.inventory.item.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Adjusts stock by a (possibly negative) delta; the resulting stock may never
 * be negative. A zero delta is rejected.
 */
public record AdjustStockRequest(

        @NotNull(message = "delta is required")
        Integer delta
) {
}
