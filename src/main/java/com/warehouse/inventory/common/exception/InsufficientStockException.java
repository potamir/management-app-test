package com.warehouse.inventory.common.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String what, int requested, int available) {
        super("Insufficient stock for %s: requested %d, available %d".formatted(what, requested, available));
    }
}
