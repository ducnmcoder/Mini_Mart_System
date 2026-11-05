package com.minimart.service.impl;

import com.minimart.config.VnPayConfig;
import com.minimart.service.VnPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class VnPayServiceImpl implements VnPayService {

    @Override
    public Map<String, String> generateVnPayQr(BigDecimal amount, String orderInfo, String ipAddress) {
        String txnRef = "MM" + System.currentTimeMillis();
        long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", VnPayConfig.VNP_VERSION);
        vnpParams.put("vnp_Command", VnPayConfig.VNP_COMMAND);
        vnpParams.put("vnp_TmnCode", VnPayConfig.VNP_TMN_CODE);
        vnpParams.put("vnp_Amount", String.valueOf(amountInCents));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo != null ? orderInfo : ("Thanh toan MiniMart don " + txnRef));
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", VnPayConfig.VNP_RETURN_URL);
        vnpParams.put("vnp_IpAddr", (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", createDate);

        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        // Sắp xếp tham số theo alphabet
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String secureHash = VnPayConfig.hmacSHA512(VnPayConfig.VNP_HASH_SECRET, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = VnPayConfig.VNP_PAY_URL + "?" + query;

        // Sinh link QR Code từ payment URL (sử dụng dịch vụ qrserver an toàn, chuẩn URL encode)
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + URLEncoder.encode(paymentUrl, StandardCharsets.UTF_8);

        Map<String, String> response = new HashMap<>();
        response.put("txnRef", txnRef);
        response.put("paymentUrl", paymentUrl);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("amount", amount.toString());
        return response;
    }
}