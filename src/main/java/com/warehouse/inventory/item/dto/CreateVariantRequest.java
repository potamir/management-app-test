package com.warehouse.inventory.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Variant payload nested in {@link CreateItemRequest}.
 */
public record CreateVariantRequest(

        @NotBlank(message = "variant name is required")
        @Size(max = 120)
        String name,

        @NotBlank(message = "variant sku is required")
        @Size(max = 60)
        String sku,

        @DecimalMin(value = "0.00", message = "variant price must be zero or positive")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        Integer stockQuantity
) {
}
