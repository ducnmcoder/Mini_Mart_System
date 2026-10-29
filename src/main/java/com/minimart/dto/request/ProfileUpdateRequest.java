package com.minimart.dto.request;

import com.minimart.dto.response.AccountProfileDto;
import com.minimart.validation.PhoneNumber;
import com.minimart.validation.ValidPasswordChange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@ValidPasswordChange
public class ProfileUpdateRequest {

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 120, message = "Họ và tên tối đa 120 ký tự.")
    private String fullName;

    @PhoneNumber
    private String phone;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public static ProfileUpdateRequest from(AccountProfileDto profile) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(profile.fullName());
        request.setPhone(profile.phone());
        return request;
    }

    public boolean isPasswordChangeRequested() {
        return hasText(currentPassword) || hasText(newPassword) || hasText(confirmPassword);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
