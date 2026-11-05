package com.minimart.service.impl;

import com.minimart.dto.request.CartCheckoutRequest;
import com.minimart.dto.request.CartItemRequest;
import com.minimart.dto.response.SaleReceiptDto;
import com.minimart.dto.response.SaleReceiptItemDto;
import com.minimart.entity.Inventory;
import com.minimart.entity.LowStockAlert;
import com.minimart.entity.Payment;
import com.minimart.entity.Product;
import com.minimart.entity.Sale;
import com.minimart.entity.SaleItem;
import com.minimart.entity.StorageLocation;
import com.minimart.entity.UserAccount;
import com.minimart.repository.InventoryRepository;
import com.minimart.repository.LowStockAlertRepository;
import com.minimart.repository.PaymentRepository;
import com.minimart.repository.ProductRepository;
import com.minimart.repository.SaleItemRepository;
import com.minimart.repository.SaleRepository;
import com.minimart.repository.StorageLocationRepository;
import com.minimart.repository.UserAccountRepository;
import com.minimart.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final LowStockAlertRepository lowStockAlertRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userAccountRepository;

    public SaleServiceImpl(SaleRepository saleRepository,
                           SaleItemRepository saleItemRepository,
                           ProductRepository productRepository,
                           InventoryRepository inventoryRepository,
                           LowStockAlertRepository lowStockAlertRepository,
                           StorageLocationRepository storageLocationRepository,
                           PaymentRepository paymentRepository,
                           UserAccountRepository userAccountRepository) {
        this.saleRepository          = saleRepository;
        this.saleItemRepository      = saleItemRepository;
        this.productRepository       = productRepository;
        this.inventoryRepository     = inventoryRepository;
        this.lowStockAlertRepository = lowStockAlertRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.paymentRepository       = paymentRepository;
        this.userAccountRepository   = userAccountRepository;
    }

    @Override
    public SaleReceiptDto checkout(Long cashierId, String cashierName, CartCheckoutRequest request) {
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        // Bước 1: Tính toán tổng giá trị và kiểm tra tồn kho
        for (CartItemRequest itemRequest : request.getItems()) {
            Product product = findActiveProductOrThrow(itemRequest.getProductId());
            Inventory inventory = findInventoryOrThrow(product);

            if (inventory.getQuantityOnHand() < itemRequest.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm '" + product.getName() + "' không đủ hàng trong kho. " +
                        "Tồn kho hiện có: " + inventory.getQuantityOnHand());
            }

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            grandTotal = grandTotal.add(lineTotal);
        }

        // Bước 2: Tạo phiếu bán hàng
        Sale sale = Sale.create(cashierId, grandTotal);
        saleRepository.save(sale);

        // Bước 3: Tạo dòng hàng, trừ tồn kho và tạo cảnh báo nếu chạm ngưỡng
        for (CartItemRequest itemRequest : request.getItems()) {
            Product product = findActiveProductOrThrow(itemRequest.getProductId());
            Inventory inventory = findInventoryOrThrow(product);
            StorageLocation location = inventory.getLocation();

            SaleItem item = SaleItem.of(sale, product, location, itemRequest.getQuantity());
            saleItemRepository.save(item);
            saleItems.add(item);

            inventory.adjustQuantity(-itemRequest.getQuantity());
            inventoryRepository.save(inventory);

            if (inventory.getQuantityOnHand() <= product.getReorderLevel()) {
                createLowStockAlertIfNeeded(product, location, inventory.getQuantityOnHand());
            }
        }

        // Bước 4: Hoàn tất phiếu bán hàng
        sale.complete();
        saleRepository.save(sale);

        // Bước 5: Ghi nhận thông tin thanh toán vào bảng PAYMENT
        String method = "VNPAY".equalsIgnoreCase(request.getPaymentMethod()) ? "VNPAY" : "CASH";
        String txnRef = "VNPAY".equals(method)
                ? (request.getVnpayTxnRef() != null ? request.getVnpayTxnRef() : ("VNP-" + System.currentTimeMillis()))
                : ("CASH-" + System.currentTimeMillis());

        Payment payment = Payment.create(sale, method, grandTotal, txnRef);
        paymentRepository.save(payment);

        BigDecimal cashGiven = request.getCashGiven() != null ? request.getCashGiven() : grandTotal;
        BigDecimal changeDue = cashGiven.compareTo(grandTotal) >= 0 ? cashGiven.subtract(grandTotal) : BigDecimal.ZERO;

        String displayMethod = "VNPAY".equals(method) ? "VNPay QR" : "Tiền mặt";
        return toReceiptDto(sale, cashierName, displayMethod, cashGiven, changeDue, saleItems);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleReceiptDto getSaleDetail(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng ID: " + saleId));

        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        String cashierName = userAccountRepository.findById(sale.getCashierId())
                .map(UserAccount::getFullName)
                .orElse("Nhân viên thu ngân");

        Payment payment = paymentRepository.findFirstBySaleIdOrderByPaidAtDesc(saleId).orElse(null);
        String displayMethod = (payment != null && "VNPAY".equalsIgnoreCase(payment.getMethod())) ? "VNPay QR" : "Tiền mặt";
        BigDecimal cashGiven = sale.getGrandTotal();
        BigDecimal changeDue = BigDecimal.ZERO;

        return toReceiptDto(sale, cashierName, displayMethod, cashGiven, changeDue, items);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleReceiptDto> searchSales(Long cashierId, String keyword, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim().replace("#", "") : null;

        return saleRepository
                .filterSales(cashierId, trimmedKeyword, fromDateTime, toDateTime, pageable)
                .map(sale -> {
                    List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
                    Payment payment = paymentRepository.findFirstBySaleIdOrderByPaidAtDesc(sale.getId()).orElse(null);
                    String displayMethod = (payment != null && "VNPAY".equalsIgnoreCase(payment.getMethod())) ? "VNPay QR" : "Tiền mặt";
                    String cashierName = userAccountRepository.findById(sale.getCashierId())
                            .map(UserAccount::getFullName)
                            .orElse("Nhân viên thu ngân");
                    return toReceiptDto(sale, cashierName, displayMethod, sale.getGrandTotal(), BigDecimal.ZERO, items);
                });
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Product findActiveProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sản phẩm ID " + productId + " không tồn tại hoặc đã ngừng kinh doanh."));
    }

    private Inventory findInventoryOrThrow(Product product) {
        return inventoryRepository.findFirstByProductId(product.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy bản ghi tồn kho cho sản phẩm: " + product.getName()));
    }

    private void createLowStockAlertIfNeeded(Product product, StorageLocation location, int currentQty) {
        boolean alreadyActive = lowStockAlertRepository
                .existsByProductIdAndStatus(product.getId(), "ACTIVE");
        if (!alreadyActive) {
            LowStockAlert alert = LowStockAlert.create(product, location, currentQty);
            lowStockAlertRepository.save(alert);
        }
    }

    private SaleReceiptDto toReceiptDto(Sale sale, String cashierName, String paymentMethod,
                                       BigDecimal cashGiven, BigDecimal changeDue, List<SaleItem> items) {
        List<SaleReceiptItemDto> receiptItems = items.stream()
                .map(item -> new SaleReceiptItemDto(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();
        return new SaleReceiptDto(
                sale.getId(),
                sale.getSaleNo(),
                cashierName,
                paymentMethod,
                cashGiven,
                changeDue,
                receiptItems,
                sale.getGrandTotal(),
                sale.getSoldAt()
        );
    }
}