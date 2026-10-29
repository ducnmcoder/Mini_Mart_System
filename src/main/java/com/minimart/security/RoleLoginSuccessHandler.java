package com.minimart.security;

import com.minimart.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class RoleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountService accountService;

    public RoleLoginSuccessHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String dashboardPath = accountService.recordSuccessfulLoginAndGetDashboardPath(authentication.getName());
        response.sendRedirect(request.getContextPath() + dashboardPath);
    }
}
