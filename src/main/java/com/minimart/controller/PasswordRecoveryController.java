package com.minimart.controller;

import com.minimart.service.PasswordRecoveryService;
import com.minimart.service.AccountService;
import com.minimart.security.MiniMartUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordRecoveryController {
    private static final String PHONE_PATTERN = "^[0-9+][0-9 .-]{7,18}$";

    private final PasswordRecoveryService recovery;
    private final MiniMartUserDetailsService userDetails;
    private final AccountService accountService;

    public PasswordRecoveryController(PasswordRecoveryService recovery, MiniMartUserDetailsService userDetails,
                                      AccountService accountService) {
        this.recovery = recovery;
        this.userDetails = userDetails;
        this.accountService = accountService;
    }

    @GetMapping("/forgot-password")
    public String phonePage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String findPhone(@RequestParam(name = "phone", defaultValue = "") String phone,
                            HttpServletRequest request, Model model) {
        String normalized = phone.trim();
        model.addAttribute("phone", normalized);
        if (!normalized.matches(PHONE_PATTERN)) {
            model.addAttribute("errorMessage", "Vui lòng nhập số điện thoại hợp lệ.");
            return "forgot-password";
        }
        var account = recovery.findActiveAccount(normalized);
        if (account.isEmpty()) {
            model.addAttribute("errorMessage", "Tài khoản có số điện thoại đó không tồn tại.");
            return "forgot-password";
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID, account.get().getId());
        session.setAttribute(PasswordRecoveryService.PENDING_PHONE, normalized);
        return "redirect:/forgot-password/reset";
    }

    @GetMapping("/forgot-password/reset")
    public String resetPage(HttpSession session) {
        if (!hasMatchingPhone(session)) {
            return "redirect:/forgot-password";
        }
        return "reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String reset(@RequestParam(name = "newPassword", defaultValue = "") String newPassword,
                        @RequestParam(name = "confirmPassword", defaultValue = "") String confirmPassword,
                        HttpServletRequest request, HttpServletResponse response,
                        HttpSession session, Model model) {
        if (!hasMatchingPhone(session)) {
            return "redirect:/forgot-password";
        }
        if (newPassword.length() < 8 || newPassword.length() > 72) {
            model.addAttribute("errorMessage", "Mật khẩu mới phải có từ 8 đến 72 ký tự.");
            return "reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            return "reset-password";
        }
        Long accountId = (Long) session.getAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID);
        String phone = (String) session.getAttribute(PasswordRecoveryService.PENDING_PHONE);
        String accountName = recovery.resetPassword(accountId, phone, newPassword);
        session.removeAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID);
        session.removeAttribute(PasswordRecoveryService.PENDING_PHONE);
        request.changeSessionId();
        var principal = userDetails.loadUserByUsername(accountName);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);
        return "redirect:" + accountService.recordSuccessfulLoginAndGetDashboardPath(principal.getUsername());
    }

    private boolean hasMatchingPhone(HttpSession session) {
        Object value = session.getAttribute(PasswordRecoveryService.PENDING_ACCOUNT_ID);
        Object phone = session.getAttribute(PasswordRecoveryService.PENDING_PHONE);
        return value instanceof Long accountId && phone instanceof String savedPhone
                && recovery.matchesPhone(accountId, savedPhone);
    }
}
