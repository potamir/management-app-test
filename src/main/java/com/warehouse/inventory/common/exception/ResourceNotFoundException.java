package com.warehouse.inventory.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s with id '%s' was not found".formatted(resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
