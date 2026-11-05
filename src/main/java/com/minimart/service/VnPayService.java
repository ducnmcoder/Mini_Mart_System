package com.minimart.service;

import java.math.BigDecimal;
import java.util.Map;

public interface VnPayService {

    /** Sinh URL thanh toán VNPay Sandbox và link ảnh mã QR Code */
    Map<String, String> generateVnPayQr(BigDecimal amount, String orderInfo, String ipAddress);
}