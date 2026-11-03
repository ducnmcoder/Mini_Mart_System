package com.minimart.controller;

import com.minimart.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes={DashboardController.class, ProfileController.class})
public class InventoryDashboardAdvice {
    private final InventoryService inventory;
    public InventoryDashboardAdvice(InventoryService inventory) { this.inventory=inventory; }
    @ModelAttribute
    public void populate(Authentication auth, Model model) {
        if (auth==null) return;
        boolean owner=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_OWNER"));
        boolean staff=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_INVENTORY_STAFF"));
        model.addAttribute("manageLocations",owner);
        model.addAttribute("manageInventory",staff);
        if (owner || staff) model.addAttribute("locations",inventory.locations());
        if (staff) {
            model.addAttribute("stockRows",inventory.stock());
            model.addAttribute("transferHistory",inventory.history());
        }
    }
}
