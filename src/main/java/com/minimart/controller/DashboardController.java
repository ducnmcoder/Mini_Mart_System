package com.minimart.controller;

import com.minimart.dto.request.ProfileUpdateRequest;
import com.minimart.dto.response.AccountProfileDto;
import com.minimart.service.AccountService;
import com.minimart.service.DashboardService;
import com.minimart.ui.DashboardPageAssembler;
import com.minimart.ui.ViewNames;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AccountService accountService;
    private final DashboardService dashboardService;
    private final DashboardPageAssembler pageAssembler;

    public DashboardController(AccountService accountService,
                               DashboardService dashboardService,
                               DashboardPageAssembler pageAssembler) {
        this.accountService = accountService;
        this.dashboardService = dashboardService;
        this.pageAssembler = pageAssembler;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        return "redirect:" + profile.role().dashboardPath();
    }

    @GetMapping({
            "/dashboard/admin",
            "/dashboard/owner",
            "/dashboard/sale-staff",
            "/dashboard/inventory-staff"
    })
    public String roleDashboard(Authentication authentication, Model model) {
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", ProfileUpdateRequest.from(profile));
        }
        model.addAttribute("page", pageAssembler.assemble(
                profile,
                dashboardService.getMetrics(profile.role())
        ));
        return ViewNames.DASHBOARD;
    }
}
