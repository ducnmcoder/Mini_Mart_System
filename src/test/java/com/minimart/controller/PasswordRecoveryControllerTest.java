package com.minimart.controller;

import com.minimart.security.MiniMartUserDetailsService;
import com.minimart.service.AccountService;
import com.minimart.service.PasswordRecoveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PasswordRecoveryControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resetCreatesLoginSessionAndRedirectsToRoleDashboard() {
        PasswordRecoveryService recovery = mock(PasswordRecoveryService.class);
        MiniMartUserDetailsService userDetails = mock(MiniMartUserDetailsService.class);
        AccountService accounts = mock(AccountService.class);
        var controller = new PasswordRecoveryController(recovery, userDetails, accounts);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var session = request.getSession();
        session.setAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID, 7L);
        session.setAttribute(PasswordRecoveryService.PENDING_PHONE, "0912345678");
        when(recovery.matchesPhone(7L, "0912345678")).thenReturn(true);
        when(recovery.resetPassword(7L, "0912345678", "new-password"))
                .thenReturn("staff");
        when(userDetails.loadUserByUsername("staff"))
                .thenReturn(User.withUsername("staff@example.com").password("hash").roles("SALE_STAFF").build());
        when(accounts.recordSuccessfulLoginAndGetDashboardPath("staff@example.com"))
                .thenReturn("/dashboard/sale-staff");

        String destination = controller.reset("new-password", "new-password", request, response,
                session, new ExtendedModelMap());

        assertThat(destination).isEqualTo("redirect:/dashboard/sale-staff");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("staff@example.com");
        assertThat(session.getAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID)).isNull();
    }
}
