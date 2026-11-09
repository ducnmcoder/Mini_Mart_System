package com.minimart.controller;

import com.minimart.ui.ViewNames;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final ObjectProvider<ClientRegistrationRepository> googleRegistration;

    public AuthController(ObjectProvider<ClientRegistrationRepository> googleRegistration) {
        this.googleRegistration = googleRegistration;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String accountName,
                        @RequestParam(required = false) String logout,
                        @RequestParam(name = "deleted", required = false) String deleted,
                        @RequestParam(name = "googleError", required = false) String googleError,
                        Authentication authentication,
                        Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        String googleMessage = switch (googleError == null ? "" : googleError) {
            case "not_found" -> "Tài khoản email Google này không tồn tại trong MiniMart.";
            case "inactive" -> "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.";
            case "unverified" -> "Email Google chưa được xác minh.";
            case "failed" -> "Không thể đăng nhập bằng Google. Vui lòng thử lại.";
            default -> null;
        };
        model.addAttribute("errorMessage", googleMessage != null ? googleMessage : error);
        model.addAttribute("googleEnabled", googleRegistration.getIfAvailable() != null);
        model.addAttribute("accountName", accountName == null ? "" : accountName);
        model.addAttribute("logoutSuccess", logout != null);
        model.addAttribute("accountDeleted", deleted != null);
        return ViewNames.LOGIN;
    }
}
