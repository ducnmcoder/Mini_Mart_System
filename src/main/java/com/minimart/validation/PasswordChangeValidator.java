package com.minimart.validation;

import com.minimart.dto.request.ProfileUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordChangeValidator implements ConstraintValidator<ValidPasswordChange, ProfileUpdateRequest> {

    @Override
    public boolean isValid(ProfileUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null || !request.isPasswordChangeRequested()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (isBlank(request.getCurrentPassword())) {
            addViolation(context, "currentPassword", "Vui lòng nhập mật khẩu hiện tại.");
            valid = false;
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            addViolation(context, "newPassword", "Mật khẩu mới phải có ít nhất 8 ký tự.");
            valid = false;
        } else if (request.getNewPassword().length() > 72) {
            addViolation(context, "newPassword", "Mật khẩu mới tối đa 72 ký tự.");
            valid = false;
        }
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            addViolation(context, "confirmPassword", "Mật khẩu xác nhận không khớp.");
            valid = false;
        }
        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
