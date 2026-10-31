package com.warehouse.inventory.common.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("A variant with SKU '%s' already exists; SKUs must be unique".formatted(sku));
    }
}
