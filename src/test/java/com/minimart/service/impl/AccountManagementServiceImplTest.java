package com.minimart.service.impl;

import com.minimart.dto.request.ManagedAccountRequest;
import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import com.minimart.entity.UserAccount;
import com.minimart.repository.RoleRepository;
import com.minimart.repository.UserAccountRepository;
import com.minimart.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountManagementServiceImplTest {
    private UserAccountRepository accounts;
    private RoleRepository roles;
    private AuditService audit;
    private AccountManagementServiceImpl management;
    private UserAccount onlyAdmin;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        accounts = mock(UserAccountRepository.class);
        roles = mock(RoleRepository.class);
        audit = mock(AuditService.class);
        adminRole = mock(Role.class);
        when(adminRole.getName()).thenReturn(AppRole.ADMIN);
        when(adminRole.isActive()).thenReturn(true);
        onlyAdmin = new UserAccount(adminRole, "admin@example.com", "hash", "Admin", null, AccountStatus.ACTIVE);
        ReflectionTestUtils.setField(onlyAdmin, "id", 1L);
        when(accounts.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(onlyAdmin));
        when(roles.findWithLockByName(AppRole.ADMIN)).thenReturn(Optional.of(adminRole));
        when(accounts.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(accounts.countByRoleAndStatusAndDeletedAtIsNull(adminRole, AccountStatus.ACTIVE)).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        management = new AccountManagementServiceImpl(accounts, roles, mock(PasswordEncoder.class), audit);
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void cannotDeleteLastActiveAdmin() {
        assertLastAdminError(() -> management.delete(1L));
    }

    @Test
    void cannotDeactivateLastActiveAdmin() {
        assertLastAdminError(() -> management.setActive(1L, false));
    }

    @Test
    void cannotChangeLastActiveAdminRole() {
        ManagedAccountRequest request = ManagedAccountRequest.from(onlyAdmin);
        request.setRole(AppRole.OWNER);
        assertLastAdminError(() -> management.update(1L, request));
    }

    @Test
    void cannotChangeLastActiveAdminStatus() {
        ManagedAccountRequest request = ManagedAccountRequest.from(onlyAdmin);
        request.setStatus(AccountStatus.INACTIVE);
        assertLastAdminError(() -> management.update(1L, request));
    }

    @Test
    void canDeleteWhenAnotherActiveAdminExists() {
        when(accounts.countByRoleAndStatusAndDeletedAtIsNull(adminRole, AccountStatus.ACTIVE)).thenReturn(2L);

        management.delete(1L);

        assertThat(onlyAdmin.getDeletedAt()).isNotNull();
        assertThat(onlyAdmin.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(accounts).saveAndFlush(onlyAdmin);
        verify(audit).record(eq(1L), eq("ACCOUNT_DELETE"), eq("USER_ACCOUNT"), eq("1"), any(), any());
    }

    @Test
    void canCreateAnAccountWithChosenRoleAndStatus() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        management = new AccountManagementServiceImpl(accounts, roles, encoder, audit);
        when(roles.findAll()).thenReturn(List.of(adminRole));
        when(encoder.encode("NewPassword123")).thenReturn("encoded");
        doAnswer(call -> {
            UserAccount created = call.getArgument(0);
            ReflectionTestUtils.setField(created, "id", 2L);
            return created;
        }).when(accounts).saveAndFlush(any(UserAccount.class));
        ManagedAccountRequest request = ManagedAccountRequest.from(onlyAdmin);
        request.setEmail("new@example.com");
        request.setPassword("NewPassword123");
        request.setConfirmPassword("NewPassword123");

        management.create(request);

        verify(accounts).saveAndFlush(argThat(a -> a.getEmail().equals("new@example.com")
                && a.getRole().getName() == AppRole.ADMIN && a.getStatus() == AccountStatus.ACTIVE
                && a.getPasswordHash().equals("encoded")));
        verify(audit).record(eq(1L), eq("ACCOUNT_CREATE"), eq("USER_ACCOUNT"), eq("2"), isNull(), any());
    }

    private void assertLastAdminError(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất một tài khoản Admin");
        verify(accounts, never()).saveAndFlush(any());
        verifyNoInteractions(audit);
    }
}
