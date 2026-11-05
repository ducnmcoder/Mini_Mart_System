package com.minimart.service;

import com.minimart.dto.request.CartCheckoutRequest;
import com.minimart.dto.response.SaleReceiptDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SaleService {

    /** Xử lý thanh toán giỏ hàng (hỗ trợ Tiền mặt hoặc VNPay Sandbox QR) */
    SaleReceiptDto checkout(Long cashierId, String cashierName, CartCheckoutRequest request);

    /** Lấy chi tiết đơn hàng đầy đủ để hiển thị modal hóa đơn */
    SaleReceiptDto getSaleDetail(Long saleId);

    /** Tìm kiếm và lọc lịch sử đơn bán theo từ khóa và khoảng thời gian */
    Page<SaleReceiptDto> searchSales(Long cashierId, String keyword, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}