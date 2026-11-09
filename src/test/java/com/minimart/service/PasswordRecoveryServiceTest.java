package com.minimart.service;

import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTest {
    private UserAccountRepository accounts;
    private PasswordEncoder encoder;
    private PasswordRecoveryService recovery;
    private UserAccount account;

    @BeforeEach
    void setUp() {
        accounts = mock(UserAccountRepository.class);
        encoder = mock(PasswordEncoder.class);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(AppRole.SALE_STAFF);
        when(role.isActive()).thenReturn(true);
        account = new UserAccount(role, "staff@example.com", "old-hash", "Staff", "0912345678", AccountStatus.ACTIVE);
        ReflectionTestUtils.setField(account, "id", 7L);
        when(accounts.findById(7L)).thenReturn(Optional.of(account));
        recovery = new PasswordRecoveryService(accounts, encoder, mock(AuditService.class));
    }

    @Test
    void refusesPasswordResetWhenAccountPhoneHasChanged() {
        assertThat(recovery.matchesPhone(7L, "0987654321")).isFalse();
        assertThatThrownBy(() -> recovery.resetPassword(7L, "0987654321", "new-password"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(encoder);
    }

    @Test
    void updatesPasswordAfterMatchingPhone() {
        when(encoder.encode("new-password")).thenReturn("new-hash");
        assertThat(recovery.matchesPhone(7L, "0912345678")).isTrue();
        assertThat(recovery.resetPassword(7L, "0912345678", "new-password"))
                .isEqualTo("staff");
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        verify(accounts).save(account);
    }
}
