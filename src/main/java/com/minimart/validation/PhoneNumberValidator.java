package com.minimart.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final String PHONE_PATTERN = "^[0-9+][0-9 .-]{7,18}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || value.trim().matches(PHONE_PATTERN);
    }
}
