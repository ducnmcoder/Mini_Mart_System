package com.minimart.controller;

import com.minimart.ui.ViewNames;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String logout,
                        @RequestParam(name = "deleted", required = false) String deleted,
                        Authentication authentication,
                        Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("errorMessage", error);
        model.addAttribute("email", email == null ? "" : email);
        model.addAttribute("logoutSuccess", logout != null);
        model.addAttribute("accountDeleted", deleted != null);
        return ViewNames.LOGIN;
    }
}
