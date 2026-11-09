package com.minimart.security;

import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MiniMartUserDetailsServiceTest {
    @Test
    void signsInWithAccountNameWhileKeepingEmailAsSessionIdentity() {
        var accounts = mock(UserAccountRepository.class);
        Role role = mock(Role.class);
        when(role.isActive()).thenReturn(true);
        when(role.getName()).thenReturn(AppRole.OWNER);
        var account = new UserAccount(role, "owner@example.com", "storeowner", "hash", "Owner",
                null, AccountStatus.ACTIVE);
        when(accounts.findByAccountNameIgnoreCase("storeowner")).thenReturn(Optional.of(account));

        var details = new MiniMartUserDetailsService(accounts).loadUserByUsername("storeowner");

        assertThat(details.getUsername()).isEqualTo("owner@example.com");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_OWNER");
    }
}
