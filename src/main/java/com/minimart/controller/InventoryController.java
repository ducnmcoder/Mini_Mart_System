package com.minimart.controller;
import com.minimart.validation.InventoryValidationException;


import com.minimart.service.AccountService;
import com.minimart.service.InventoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InventoryController {
    private final InventoryService inventory;
    private final AccountService accounts;
    public InventoryController(InventoryService inventory, AccountService accounts) { this.inventory=inventory; this.accounts=accounts; }

    @PostMapping("/owner/locations/save")
    @PreAuthorize("hasRole('OWNER')")
    public String save(@RequestParam(required=false) Long id, @RequestParam String code,
                       @RequestParam String name, @RequestParam(defaultValue="") String description,
                       Authentication auth, RedirectAttributes flash) {
        return perform(() -> inventory.saveLocation(id,code,name,description,accounts.getProfile(auth.getName()).id()),
                "/dashboard/owner#locations","Đã lưu gian hàng.",flash);
    }
    @PostMapping("/owner/locations/{id}/delete")
    @PreAuthorize("hasRole('OWNER')")
    public String delete(@PathVariable long id, Authentication auth, RedirectAttributes flash) {
        return perform(() -> inventory.deleteLocation(id,accounts.getProfile(auth.getName()).id()),
                "/dashboard/owner#locations","Đã xóa gian hàng khỏi danh sách sử dụng.",flash);
    }
    @PostMapping("/inventory/transfers")
    @PreAuthorize("hasRole('INVENTORY_STAFF')")
    public String transfer(@RequestParam long product, @RequestParam long source, @RequestParam long target,
                           @RequestParam int quantity, @RequestParam String reason,
                           Authentication auth, RedirectAttributes flash) {
        return perform(() -> inventory.transfer(product,source,target,quantity,reason,accounts.getProfile(auth.getName()).id()),
                "/dashboard/inventory-staff#inventory","Đã chuyển hàng và ghi lịch sử tồn kho.",flash);
    }
    private String perform(Runnable action, String destination, String message, RedirectAttributes flash) {
        try { action.run(); flash.addFlashAttribute("successMessage",message); }
        catch (InventoryValidationException e) { flash.addFlashAttribute("inventoryError",e.getMessage()); }
        catch (DataIntegrityViolationException e) { flash.addFlashAttribute("inventoryError","Mã gian hàng đã được dùng hoặc dữ liệu không hợp lệ. Vui lòng kiểm tra lại."); }
        return "redirect:"+destination;
    }
}
