package com.minimart.dto.request;

import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.UserAccount;
import com.minimart.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ManagedAccountRequest {
    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Vui lòng nhập tên tài khoản.")
    @Size(max = 60)
    private String accountName;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 120)
    private String fullName;

    @PhoneNumber
    private String phone;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private AppRole role;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private AccountStatus status = AccountStatus.ACTIVE;

    private String password;
    private String confirmPassword;

    public static ManagedAccountRequest from(UserAccount account) {
        ManagedAccountRequest request = new ManagedAccountRequest();
        request.email = account.getEmail();
        request.accountName = account.getAccountName();
        request.fullName = account.getFullName();
        request.phone = account.getPhone();
        request.role = account.getRole().getName();
        request.status = account.getStatus();
        return request;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public AppRole getRole() { return role; }
    public void setRole(AppRole role) { this.role = role; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
