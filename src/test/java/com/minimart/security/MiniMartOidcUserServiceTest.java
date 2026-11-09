package com.minimart.security;

import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MiniMartOidcUserServiceTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final MiniMartOidcUserService service = new MiniMartOidcUserService(accounts);

    @Test
    void rejectsUnknownEmail() {
        OidcUser google = googleUser("unknown@example.com", true);
        when(accounts.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.localUser(google))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
                        error -> assertThat(error.getError().getErrorCode()).isEqualTo("account_not_found"));
    }

    @Test
    void rejectsUnverifiedEmailBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.localUser(googleUser("admin@example.com", false)))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
                        error -> assertThat(error.getError().getErrorCode()).isEqualTo("email_unverified"));
        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsInactiveAccount() {
        Role role = mock(Role.class);
        when(role.isActive()).thenReturn(true);
        UserAccount account = new UserAccount(role, "admin@example.com", "hash", "Admin", null, AccountStatus.INACTIVE);
        when(accounts.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.localUser(googleUser("admin@example.com", true)))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
                        error -> assertThat(error.getError().getErrorCode()).isEqualTo("account_inactive"));
    }

    @Test
    void grantsRoleFromLocalAccountAndUsesEmailAsPrincipalName() {
        Role role = mock(Role.class);
        when(role.isActive()).thenReturn(true);
        when(role.getName()).thenReturn(AppRole.OWNER);
        UserAccount account = new UserAccount(role, "owner@example.com", "hash", "Owner", null, AccountStatus.ACTIVE);
        when(accounts.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(account));
        OidcUser google = googleUser("owner@example.com", true);

        OidcUser local = service.localUser(google);

        assertThat(local.getName()).isEqualTo("owner@example.com");
        assertThat(local.getAuthorities()).extracting("authority").containsExactly("ROLE_OWNER");
    }

    private OidcUser googleUser(String email, boolean verified) {
        OidcUser user = mock(OidcUser.class);
        when(user.getEmailVerified()).thenReturn(verified);
        if (verified) {
            when(user.getEmail()).thenReturn(email);
            when(user.getIdToken()).thenReturn(new OidcIdToken("test-token", Instant.now(),
                    Instant.now().plusSeconds(60), Map.of("sub", "google-subject", "email", email,
                            "email_verified", true)));
        }
        return user;
    }
}
