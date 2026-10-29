package com.minimart.validation;

public class ProfileValidationException extends RuntimeException {

    private final String field;

    public ProfileValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
