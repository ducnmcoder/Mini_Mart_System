package com.minimart.dto.response;

import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.UserAccount;

import java.time.LocalDateTime;

public record AccountProfileDto(
        Long id,
        String email,
        String fullName,
        String phone,
        AccountStatus status,
        AppRole role,
        LocalDateTime lastLoginAt
) {
    public static AccountProfileDto from(UserAccount account) {
        return new AccountProfileDto(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getStatus(),
                account.getRole().getName(),
                account.getLastLoginAt()
        );
    }
}
