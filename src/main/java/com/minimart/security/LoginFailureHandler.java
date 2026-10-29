package com.minimart.security;

import com.minimart.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final UserAccountRepository accountRepository;

    public LoginFailureHandler(UserAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String error;

        if ((email == null || email.isBlank()) && (password == null || password.isBlank())) {
            error = "Vui lòng nhập tài khoản và mật khẩu.";
        } else if (email == null || email.isBlank()) {
            error = "Vui lòng nhập tài khoản.";
        } else if (password == null || password.isBlank()) {
            error = "Vui lòng nhập mật khẩu.";
        } else if (accountRepository.findByEmailIgnoreCase(email.trim()).isEmpty()) {
            error = "Tài khoản không tồn tại. Vui lòng kiểm tra lại.";
        } else {
            error = "Mật khẩu không đúng hoặc tài khoản chưa được kích hoạt.";
        }

        String encodedEmail = email == null ? "" : URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedError = URLEncoder.encode(error, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?error=" + encodedError + "&email=" + encodedEmail);
    }
}
