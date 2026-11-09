package com.minimart.service.impl;

import com.minimart.dto.request.ManagedAccountRequest;
import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import com.minimart.entity.UserAccount;
import com.minimart.repository.RoleRepository;
import com.minimart.repository.UserAccountRepository;
import com.minimart.service.AccountManagementService;
import com.minimart.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AccountManagementServiceImpl implements AccountManagementService {
    private final UserAccountRepository accounts;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountManagementServiceImpl(UserAccountRepository accounts, RoleRepository roles,
                                        PasswordEncoder encoder, AuditService audit) {
        this.accounts = accounts;
        this.roles = roles;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> list() { currentAdmin(); return accounts.findByDeletedAtIsNullOrderByIdAsc(); }

    @Override
    @Transactional(readOnly = true)
    public UserAccount get(Long id) { currentAdmin(); return require(id); }

    @Override
    @Transactional
    public void create(ManagedAccountRequest request) {
        lockAdminRole();
        Long actorId = currentAdmin().getId();
        validate(request, null, true);
        Role role = requireRole(request.getRole());
        UserAccount account = new UserAccount(role, email(request.getEmail()), request.getAccountName().trim(), encoder.encode(request.getPassword()),
                request.getFullName().trim(), phone(request.getPhone()), request.getStatus());
        accounts.saveAndFlush(account);
        audit.record(actorId, "ACCOUNT_CREATE", "USER_ACCOUNT", account.getId().toString(),
                null, snapshot(account));
    }

    @Override
    @Transactional
    public void update(Long id, ManagedAccountRequest request) {
        // Serialize admin mutations on the same role row before reading the target/count.
        Role adminRole = lockAdminRole();
        Long actorId = currentAdmin().getId();
        UserAccount account = require(id);
        validate(request, id, false);
        if (isActiveAdmin(account) && (request.getRole() != AppRole.ADMIN || request.getStatus() != AccountStatus.ACTIVE)) {
            requireAnotherAdmin(adminRole);
        }
        Map<String, Object> before = snapshot(account);
        account.updateByAdmin(requireRole(request.getRole()), email(request.getEmail()), request.getAccountName().trim(),
                request.getFullName().trim(), phone(request.getPhone()), request.getStatus());
        if (hasText(request.getPassword())) account.changePassword(encoder.encode(request.getPassword()));
        accounts.saveAndFlush(account);
        audit.record(actorId, "ACCOUNT_UPDATE", "USER_ACCOUNT", id.toString(), before, snapshot(account));
    }

    @Override
    @Transactional
    public void setActive(Long id, boolean active) {
        Role adminRole = lockAdminRole();
        Long actorId = currentAdmin().getId();
        UserAccount account = require(id);
        if (!active && isActiveAdmin(account)) requireAnotherAdmin(adminRole);
        Map<String, Object> before = snapshot(account);
        account.updateByAdmin(account.getRole(), account.getEmail(), account.getAccountName(), account.getFullName(), account.getPhone(),
                active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        accounts.saveAndFlush(account);
        audit.record(actorId, active ? "ACCOUNT_ACTIVATE" : "ACCOUNT_DEACTIVATE",
                "USER_ACCOUNT", id.toString(), before, snapshot(account));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role adminRole = lockAdminRole();
        Long actorId = currentAdmin().getId();
        UserAccount account = require(id);
        if (isActiveAdmin(account)) requireAnotherAdmin(adminRole);
        Map<String, Object> before = snapshot(account);
        account.deleteByAdmin();
        accounts.saveAndFlush(account);
        audit.record(actorId, "ACCOUNT_DELETE", "USER_ACCOUNT", id.toString(), before, snapshot(account));
    }

    private void validate(ManagedAccountRequest request, Long id, boolean creating) {
        String normalized = email(request.getEmail());
        if (request.getAccountName() == null || request.getAccountName().isBlank())
            throw new IllegalArgumentException("Tên tài khoản là bắt buộc.");
        if (accounts.existsByAccountNameIgnoreCaseAndIdNot(request.getAccountName().trim(), id == null ? -1L : id))
            throw new IllegalArgumentException("Tên tài khoản đã được sử dụng.");
        if (accounts.existsByEmailIgnoreCaseAndIdNot(normalized, id == null ? -1L : id))
            throw new IllegalArgumentException("Email đã được sử dụng.");
        if (phone(request.getPhone()) != null && accounts.existsByPhoneAndIdNot(phone(request.getPhone()), id == null ? -1L : id))
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
        if (request.getRole() == null || request.getStatus() == null)
            throw new IllegalArgumentException("Vai trò và trạng thái là bắt buộc.");
        if (creating || hasText(request.getPassword()) || hasText(request.getConfirmPassword())) {
            if (request.getPassword() == null || request.getPassword().length() < 8 || request.getPassword().length() > 72)
                throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
            if (!request.getPassword().equals(request.getConfirmPassword()))
                throw new IllegalArgumentException("Xác nhận mật khẩu không khớp.");
        }
    }

    private Role lockAdminRole() {
        return roles.findWithLockByName(AppRole.ADMIN).orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò Admin."));
    }

    private void requireAnotherAdmin(Role adminRole) {
        if (accounts.countByRoleAndStatusAndDeletedAtIsNull(adminRole, AccountStatus.ACTIVE) <= 1)
            throw new IllegalArgumentException("Phải luôn còn ít nhất một tài khoản Admin đang hoạt động.");
    }

    private boolean isActiveAdmin(UserAccount account) {
        return account.getRole().getName() == AppRole.ADMIN && account.getStatus() == AccountStatus.ACTIVE;
    }

    private Role requireRole(AppRole role) {
        Role found = roles.findAll().stream().filter(item -> item.getName() == role).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
        if (!found.isActive()) throw new IllegalArgumentException("Vai trò đã ngừng sử dụng.");
        return found;
    }

    private UserAccount currentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return accounts.findByEmailIgnoreCase(email)
                .filter(a -> a.getDeletedAt() == null && a.isLoginAllowed() && a.getRole().getName() == AppRole.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản Admin không còn quyền truy cập."));
    }

    private UserAccount require(Long id) {
        return accounts.findById(id).filter(a -> a.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
    }

    private String email(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String phone(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private Map<String, Object> snapshot(UserAccount account) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("email", account.getEmail());
        values.put("accountName", account.getAccountName());
        values.put("fullName", account.getFullName());
        values.put("phone", account.getPhone());
        values.put("role", account.getRole().getName().name());
        values.put("status", account.getStatus().name());
        values.put("deletedAt", account.getDeletedAt());
        return values;
    }
}
