package com.minimart.controller;

import com.minimart.dto.request.ManagedAccountRequest;
import com.minimart.entity.AccountStatus;
import com.minimart.entity.AppRole;
import com.minimart.entity.UserAccount;
import com.minimart.service.AccountManagementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminAccountController {
    private final AccountManagementService management;

    public AdminAccountController(AccountManagementService management) {
        this.management = management;
    }

    @GetMapping("/admin/accounts")
    public String list(Model model) {
        model.addAttribute("accounts", management.list());
        return "admin-accounts";
    }

    @GetMapping("/admin/accounts/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("accountRequest")) model.addAttribute("accountRequest", new ManagedAccountRequest());
        formModel(model, null);
        return "admin-account-form";
    }

    @GetMapping("/admin/accounts/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserAccount account = management.get(id);
        if (!model.containsAttribute("accountRequest")) model.addAttribute("accountRequest", ManagedAccountRequest.from(account));
        formModel(model, id);
        return "admin-account-form";
    }

    @PostMapping("/admin/accounts")
    public String create(@Valid @ModelAttribute("accountRequest") ManagedAccountRequest request,
                         BindingResult errors, Model model,
                         RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                management.create(request);
                redirect.addFlashAttribute("successMessage", "Đã tạo tài khoản.");
                return "redirect:/admin/accounts";
            } catch (IllegalArgumentException exception) {
                model.addAttribute("accountError", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                model.addAttribute("accountError", "Email hoặc số điện thoại đã được sử dụng.");
            }
        }
        formModel(model, null);
        return "admin-account-form";
    }

    @PostMapping("/admin/accounts/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("accountRequest") ManagedAccountRequest request,
                         BindingResult errors, Authentication authentication, HttpServletRequest servletRequest,
                         Model model, RedirectAttributes redirect) throws ServletException {
        UserAccount account = management.get(id);
        if (!errors.hasErrors()) {
            try {
                boolean self = account.getEmail().equalsIgnoreCase(authentication.getName());
                boolean reauthenticate = self && (!account.getEmail().equalsIgnoreCase(request.getEmail().trim())
                        || request.getRole() != AppRole.ADMIN || request.getStatus() != AccountStatus.ACTIVE
                        || (request.getPassword() != null && !request.getPassword().isBlank()));
                management.update(id, request);
                if (reauthenticate) {
                    servletRequest.logout();
                    return "redirect:/login?logout";
                }
                redirect.addFlashAttribute("successMessage", "Đã cập nhật tài khoản.");
                return "redirect:/admin/accounts";
            } catch (IllegalArgumentException exception) {
                model.addAttribute("accountError", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                model.addAttribute("accountError", "Email hoặc số điện thoại đã được sử dụng.");
            }
        }
        formModel(model, id);
        return "admin-account-form";
    }

    @PostMapping("/admin/accounts/{id}/status")
    public String status(@PathVariable Long id, Authentication authentication, HttpServletRequest servletRequest,
                         RedirectAttributes redirect) throws ServletException {
        UserAccount account = management.get(id);
        boolean active = account.getStatus() != AccountStatus.ACTIVE;
        try {
            management.setActive(id, active);
            if (!active && account.getEmail().equalsIgnoreCase(authentication.getName())) {
                servletRequest.logout();
                return "redirect:/login?logout";
            }
            redirect.addFlashAttribute("successMessage", active ? "Đã kích hoạt tài khoản." : "Đã ngừng hoạt động tài khoản.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("accountError", exception.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, HttpServletRequest servletRequest,
                         RedirectAttributes redirect) throws ServletException {
        UserAccount account = management.get(id);
        try {
            management.delete(id);
            if (account.getEmail().equalsIgnoreCase(authentication.getName())) {
                servletRequest.logout();
                return "redirect:/login?logout";
            }
            redirect.addFlashAttribute("successMessage", "Đã xóa tài khoản khỏi danh sách.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("accountError", exception.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    private void formModel(Model model, Long id) {
        model.addAttribute("accountId", id);
        model.addAttribute("roles", AppRole.values());
        model.addAttribute("statuses", AccountStatus.values());
    }
}
