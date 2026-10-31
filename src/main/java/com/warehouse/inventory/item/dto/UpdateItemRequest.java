package com.warehouse.inventory.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateItemRequest(

        @NotBlank(message = "name is required")
        @Size(max = 120)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must be zero or positive")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price
) {
}
