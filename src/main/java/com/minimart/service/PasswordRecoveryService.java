package com.minimart.service;

import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class PasswordRecoveryService {
    public static final String PENDING_ACCOUNT_ID = "passwordRecoveryAccountId";
    public static final String PENDING_PHONE = "passwordRecoveryPhone";

    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public PasswordRecoveryService(UserAccountRepository accounts, PasswordEncoder encoder, AuditService audit) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findActiveAccount(String phone) {
        return accounts.findByPhoneAndDeletedAtIsNull(phone.trim()).filter(UserAccount::isLoginAllowed);
    }

    @Transactional(readOnly = true)
    public boolean matchesPhone(Long accountId, String phone) {
        return accountId != null && phone != null && accounts.findById(accountId)
                .filter(UserAccount::isLoginAllowed)
                .map(account -> phone.equals(account.getPhone()))
                .orElse(false);
    }

    @Transactional
    public String resetPassword(Long accountId, String phone, String newPassword) {
        UserAccount account = accounts.findById(accountId)
                .filter(UserAccount::isLoginAllowed)
                .filter(found -> phone.equals(found.getPhone()))
                .orElseThrow(() -> new IllegalStateException("Tài khoản không còn hợp lệ."));
        account.changePassword(encoder.encode(newPassword));
        accounts.save(account);
        audit.record(account.getId(), "PASSWORD_RESET", "USER_ACCOUNT", account.getId().toString(),
              Map.of(), Map.of("passwordChanged", true));
        return account.getAccountName();
    }
}
