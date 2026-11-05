package com.minimart.controller;

import com.minimart.dto.request.InventoryAdjustRequest;
import com.minimart.dto.request.ProductSaveRequest;
import com.minimart.dto.response.AccountProfileDto;
import com.minimart.dto.response.ProductDetailDto;
import com.minimart.service.AccountService;
import com.minimart.service.ProductService;
import com.minimart.ui.ViewNames;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner/products")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class ProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final AccountService accountService;

    public ProductController(ProductService productService, AccountService accountService) {
        this.productService = productService;
        this.accountService  = accountService;
    }

    /** Danh sach san pham */
    @GetMapping
    public String listProducts(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               Authentication authentication,
                               Model model) {
        Page<ProductDetailDto> products = productService.getProducts(
                keyword,
                PageRequest.of(page, PAGE_SIZE, Sort.by("name").ascending())
        );
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        model.addAttribute("profile", profile);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        return ViewNames.PRODUCT_LIST;
    }

    /** Form them moi */
    @GetMapping("/new")
    public String showCreateForm(Authentication authentication, Model model) {
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", new ProductSaveRequest());
        }
        prepareFormModel(model, authentication, null);
        return ViewNames.PRODUCT_FORM;
    }

    /** Luu san pham moi */
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("productRequest") ProductSaveRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, authentication, null);
            return ViewNames.PRODUCT_FORM;
        }
        try {
            productService.createProduct(request);
            redirectAttributes.addFlashAttribute("successMessage", "Da them san pham thanh cong!");
            return "redirect:/owner/products";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("barcode", "barcode.duplicate", ex.getMessage());
            prepareFormModel(model, authentication, null);
            return ViewNames.PRODUCT_FORM;
        }
    }

    /** Form chinh sua */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               Authentication authentication,
                               Model model) {
        ProductDetailDto product = productService.getProductById(id);
        if (!model.containsAttribute("productRequest")) {
            ProductSaveRequest request = new ProductSaveRequest();
            request.setName(product.name());
            request.setBarcode(product.barcode());
            request.setPrice(product.price());
            request.setCategoryId(product.categoryId());
            request.setImageUrl(product.imageUrl());
            request.setStatus(product.status());
            model.addAttribute("productRequest", request);
        }
        model.addAttribute("productId", id);
        prepareFormModel(model, authentication, product);
        return ViewNames.PRODUCT_FORM;
    }

    /** Luu cap nhat san pham */
    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productRequest") ProductSaveRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            ProductDetailDto product = productService.getProductById(id);
            model.addAttribute("productId", id);
            prepareFormModel(model, authentication, product);
            return ViewNames.PRODUCT_FORM;
        }
        try {
            productService.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Da cap nhat san pham thanh cong!");
            return "redirect:/owner/products";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("barcode", "barcode.duplicate", ex.getMessage());
            ProductDetailDto product = productService.getProductById(id);
            model.addAttribute("productId", id);
            prepareFormModel(model, authentication, product);
            return ViewNames.PRODUCT_FORM;
        }
    }

    /** Vo hieu hoa san pham */
    @PostMapping("/{id}/deactivate")
    public String deactivateProduct(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        productService.deactivateProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Da vo hieu hoa san pham.");
        return "redirect:/owner/products";
    }

    /** Form cap nhat ton kho */
    @GetMapping("/{id}/inventory")
    public String showInventoryForm(@PathVariable Long id,
                                    Authentication authentication,
                                    Model model) {
        ProductDetailDto product = productService.getProductById(id);
        if (!model.containsAttribute("inventoryRequest")) {
            InventoryAdjustRequest request = new InventoryAdjustRequest();
            request.setNewQuantity(product.quantityOnHand());
            request.setLowStockThreshold(product.lowStockThreshold());
            model.addAttribute("inventoryRequest", request);
        }
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        model.addAttribute("profile", profile);
        model.addAttribute("product", product);
        return ViewNames.PRODUCT_INVENTORY_FORM;
    }

    /** Luu cap nhat ton kho */
    @PostMapping("/{id}/inventory")
    public String updateInventory(@PathVariable Long id,
                                  @Valid @ModelAttribute("inventoryRequest") InventoryAdjustRequest request,
                                  BindingResult bindingResult,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            ProductDetailDto product = productService.getProductById(id);
            AccountProfileDto profile = accountService.getProfile(authentication.getName());
            model.addAttribute("profile", profile);
            model.addAttribute("product", product);
            return ViewNames.PRODUCT_INVENTORY_FORM;
        }
        productService.updateInventory(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Da cap nhat ton kho thanh cong!");
        return "redirect:/owner/products";
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private void prepareFormModel(Model model, Authentication authentication, ProductDetailDto existingProduct) {
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        model.addAttribute("profile", profile);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("allStatuses", com.minimart.entity.ProductStatus.values());
        if (existingProduct != null) {
            model.addAttribute("existingProduct", existingProduct);
        }
    }
}
