package com.minimart.controller;

import com.minimart.dto.request.CartCheckoutRequest;
import com.minimart.dto.response.AccountProfileDto;
import com.minimart.dto.response.ProductSearchResultDto;
import com.minimart.dto.response.SaleReceiptDto;
import com.minimart.service.AccountService;
import com.minimart.service.DashboardService;
import com.minimart.service.ProductService;
import com.minimart.service.SaleService;
import com.minimart.service.VnPayService;
import com.minimart.ui.ViewNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sale")
@PreAuthorize("hasAnyRole('SALE_STAFF', 'OWNER', 'ADMIN')")
public class SaleController {

    private final SaleService saleService;
    private final ProductService productService;
    private final AccountService accountService;
    private final DashboardService dashboardService;
    private final VnPayService vnPayService;

    public SaleController(SaleService saleService,
                          ProductService productService,
                          AccountService accountService,
                          DashboardService dashboardService,
                          VnPayService vnPayService) {
        this.saleService      = saleService;
        this.productService   = productService;
        this.accountService   = accountService;
        this.dashboardService = dashboardService;
        this.vnPayService     = vnPayService;
    }

    /** Màn hình POS chính */
    @GetMapping("/pos")
    public String posPage(Authentication authentication, Model model) {
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        model.addAttribute("profile", profile);
        model.addAttribute("metrics", dashboardService.getMetrics(profile.role(), profile.id()));
        return ViewNames.SALE_POS;
    }

    /** API tìm kiếm sản phẩm (AJAX, trả JSON) */
    @GetMapping("/pos/search")
    public ResponseEntity<List<ProductSearchResultDto>> searchProducts(
            @RequestParam(defaultValue = "") String q) {
        List<ProductSearchResultDto> results = productService.searchForPos(q);
        return ResponseEntity.ok(results);
    }

    /** API tạo mã QR thanh toán VNPay Sandbox (Hỗ trợ cả POST và GET để tránh 405) */
    @RequestMapping(value = "/pos/vnpay-qr", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> generateVnPayQr(@RequestBody(required = false) Map<String, Object> payload,
                                             @RequestParam(required = false) BigDecimal amount,
                                             @RequestParam(required = false) String orderInfo,
                                             HttpServletRequest request) {
        try {
            BigDecimal finalAmount = BigDecimal.ZERO;
            if (payload != null && payload.get("amount") != null) {
                finalAmount = new BigDecimal(payload.get("amount").toString());
            } else if (amount != null) {
                finalAmount = amount;
            }

            if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Số tiền thanh toán phải lớn hơn 0"));
            }

            String finalOrderInfo = (payload != null && payload.get("orderInfo") != null)
                    ? payload.get("orderInfo").toString()
                    : (orderInfo != null ? orderInfo : "Thanh toan MiniMart POS");
            String ipAddress = request.getRemoteAddr();

            Map<String, String> qrData = vnPayService.generateVnPayQr(finalAmount, finalOrderInfo, ipAddress);
            return ResponseEntity.ok(qrData);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo QR VNPay: " + ex.getMessage()));
        }
    }

    /** Xử lý checkout (AJAX, nhận và trả JSON) */
    @PostMapping("/pos/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CartCheckoutRequest request,
                                      BindingResult bindingResult,
                                      Authentication authentication) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(e ->
                    errors.put(e.getField(), e.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            AccountProfileDto profile = accountService.getProfile(authentication.getName());
            SaleReceiptDto receipt = saleService.checkout(profile.id(), profile.fullName(), request);
            return ResponseEntity.ok(receipt);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /** Lịch sử đơn bán của nhân viên có lọc theo ngày và từ khóa */
    @GetMapping("/receipts")
    public String receiptsPage(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                               @RequestParam(defaultValue = "0") int page,
                               Authentication authentication,
                               Model model) {
        AccountProfileDto profile = accountService.getProfile(authentication.getName());
        Long cashierFilterId = "SALE_STAFF".equals(profile.role().name()) ? profile.id() : null;
        Page<SaleReceiptDto> receipts = saleService.searchSales(
                cashierFilterId,
                keyword,
                fromDate,
                toDate,
                PageRequest.of(page, 15)
        );
        model.addAttribute("profile", profile);
        model.addAttribute("receipts", receipts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("currentPage", page);
        return ViewNames.SALE_RECEIPTS;
    }

    /** API lấy chi tiết một đơn hàng để xem lại hóa đơn */
    @GetMapping("/receipts/{id}/detail")
    public ResponseEntity<?> getReceiptDetail(@PathVariable Long id) {
        try {
            SaleReceiptDto detail = saleService.getSaleDetail(id);
            return ResponseEntity.ok(detail);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /** API ghi nhận yêu cầu hoàn trả đơn hàng */
    @PostMapping("/receipts/{id}/refund")
    public ResponseEntity<?> requestRefund(@PathVariable Long id) {
        // Ghi nhận yêu cầu hoàn tiền demo
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã ghi nhận yêu cầu hoàn trả cho đơn hàng #" + id + ". Chức năng đang được phát triển trong phiên bản tiếp theo."
        ));
    }
}