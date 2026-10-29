package com.minimart.controller;

import com.minimart.dto.request.ProfileUpdateRequest;
import com.minimart.dto.response.AccountProfileDto;
import com.minimart.service.AccountService;
import com.minimart.service.DashboardService;
import com.minimart.ui.DashboardPageAssembler;
import com.minimart.ui.ViewNames;
import com.minimart.validation.ProfileValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final AccountService accountService;
    private final DashboardService dashboardService;
    private final DashboardPageAssembler pageAssembler;

    public ProfileController(AccountService accountService,
                             DashboardService dashboardService,
                             DashboardPageAssembler pageAssembler) {
        this.accountService = accountService;
        this.dashboardService = dashboardService;
        this.pageAssembler = pageAssembler;
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        AccountProfileDto currentProfile = accountService.getProfile(authentication.getName());
        if (!bindingResult.hasErrors()) {
            try {
                accountService.updateProfile(authentication.getName(), request);
            } catch (ProfileValidationException exception) {
                bindingResult.rejectValue(exception.getField(), "profile.invalid", exception.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("page", pageAssembler.assemble(
                    currentProfile,
                    dashboardService.getMetrics(currentProfile.role(), currentProfile.id())
            ));
            return ViewNames.DASHBOARD;
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "Thông tin cá nhân đã được lưu xuống cơ sở dữ liệu."
        );
        return "redirect:" + currentProfile.role().dashboardPath() + "#profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(@RequestParam(name = "deletePassword", defaultValue = "") String password,
                                Authentication authentication,
                                HttpServletRequest servletRequest,
                                RedirectAttributes redirectAttributes) throws ServletException {
        AccountProfileDto currentProfile = accountService.getProfile(authentication.getName());
        try {
            accountService.deactivateAccount(authentication.getName(), password);
        } catch (ProfileValidationException exception) {
            redirectAttributes.addFlashAttribute("deleteError", exception.getMessage());
            return "redirect:" + currentProfile.role().dashboardPath() + "#profile";
        }

        servletRequest.logout();
        return "redirect:/login?deleted";
    }
}
