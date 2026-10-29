package com.minimart.service.impl;

import com.minimart.dto.request.ProfileUpdateRequest;
import com.minimart.dto.response.AccountProfileDto;
import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import com.minimart.service.AccountService;
import com.minimart.service.AuditService;
import com.minimart.validation.ProfileValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountServiceImpl(UserAccountRepository accountRepository,
                              PasswordEncoder passwordEncoder,
                              AuditService auditService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountProfileDto getProfile(String email) {
        return AccountProfileDto.from(requireAccount(email));
    }

    @Override
    @Transactional
    public AccountProfileDto updateProfile(String email, ProfileUpdateRequest request) {
        UserAccount account = requireAccount(email);
        String normalizedPhone = normalize(request.getPhone());

        if (normalizedPhone != null && accountRepository.existsByPhoneAndIdNot(normalizedPhone, account.getId())) {
            throw new ProfileValidationException("phone", "Số điện thoại đã được một tài khoản khác sử dụng.");
        }
        if (request.isPasswordChangeRequested()
                && !passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new ProfileValidationException("currentPassword", "Mật khẩu hiện tại không đúng.");
        }

        Map<String, Object> oldValues = snapshot(account);
        account.updateProfile(request.getFullName().trim(), normalizedPhone);
        if (request.isPasswordChangeRequested()) {
            account.changePassword(passwordEncoder.encode(request.getNewPassword()));
        }
        UserAccount saved = accountRepository.save(account);
        auditService.record(saved.getId(), "PROFILE_UPDATE", "USER_ACCOUNT", saved.getId().toString(),
                oldValues, snapshot(saved));
        return AccountProfileDto.from(saved);
    }

    @Override
    @Transactional
    public void deactivateAccount(String email, String password) {
        UserAccount account = requireAccount(email);
        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new ProfileValidationException("deletePassword", "Mật khẩu xác nhận không đúng.");
        }

        Map<String, Object> oldValues = snapshot(account);
        account.deactivate();
        accountRepository.save(account);
        auditService.record(account.getId(), "ACCOUNT_DEACTIVATE", "USER_ACCOUNT", account.getId().toString(),
                oldValues, snapshot(account));
    }

    @Override
    @Transactional
    public String recordSuccessfulLoginAndGetDashboardPath(String email) {
        UserAccount account = requireAccount(email);
        account.recordSuccessfulLogin();
        accountRepository.save(account);
        return account.getRole().getName().dashboardPath();
    }

    private UserAccount requireAccount(String email) {
        return accountRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new IllegalStateException("Tài khoản không còn tồn tại."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> snapshot(UserAccount account) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fullName", account.getFullName());
        values.put("phone", account.getPhone());
        values.put("email", account.getEmail());
        values.put("status", account.getStatus().name());
        return values;
    }
}
