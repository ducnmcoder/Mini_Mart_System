package com.minimart.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String reason = "failed";
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            reason = switch (oauthException.getError().getErrorCode()) {
                case "account_not_found" -> "not_found";
                case "account_inactive" -> "inactive";
                case "email_unverified" -> "unverified";
                default -> "failed";
            };
        }
        response.sendRedirect(request.getContextPath() + "/login?googleError=" + reason);
    }
}
