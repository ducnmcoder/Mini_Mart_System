package com.minimart.security;

import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserAccountRepository accounts;

    public AccountSessionFilter(UserAccountRepository accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && !request.getServletPath().equals("/logout")) {
            UserAccount account = accounts.findByEmailIgnoreCase(auth.getName()).orElse(null);
            if (account == null || !account.isLoginAllowed()
                    || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + account.getRole().getName().name()))) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
                response.sendRedirect(request.getContextPath() + "/login?logout");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
