package com.minimart.validation;

/** Business validation, kept separate from persistence API errors. */
public class InventoryValidationException extends RuntimeException {
    public InventoryValidationException(String message) { super(message); }
}
