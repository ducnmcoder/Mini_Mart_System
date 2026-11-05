package com.minimart.service.impl;

import com.minimart.dto.request.InventoryAdjustRequest;
import com.minimart.dto.request.ProductSaveRequest;
import com.minimart.dto.response.ProductDetailDto;
import com.minimart.dto.response.ProductSearchResultDto;
import com.minimart.entity.Category;
import com.minimart.entity.Inventory;
import com.minimart.entity.Product;
import com.minimart.entity.ProductStatus;
import com.minimart.entity.StorageLocation;
import com.minimart.repository.CategoryRepository;
import com.minimart.repository.InventoryRepository;
import com.minimart.repository.ProductRepository;
import com.minimart.repository.StorageLocationRepository;
import com.minimart.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final StorageLocationRepository storageLocationRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              InventoryRepository inventoryRepository,
                              CategoryRepository categoryRepository,
                              StorageLocationRepository storageLocationRepository) {
        this.productRepository         = productRepository;
        this.inventoryRepository       = inventoryRepository;
        this.categoryRepository        = categoryRepository;
        this.storageLocationRepository = storageLocationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDetailDto> getProducts(String keyword, Pageable pageable) {
        String trimmed = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return productRepository
                .findByKeywordAndStatus(trimmed, ProductStatus.ACTIVE, pageable)
                .map(this::toDetailDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDto getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return toDetailDto(product);
    }

    @Override
    public ProductDetailDto createProduct(ProductSaveRequest request) {
        validateBarcodeUnique(request.getBarcode(), null);

        Category category = resolveCategory(request.getCategoryId());
        Product product = Product.create(
                category,
                request.getName(),
                request.getBarcode(),
                request.getPrice(),
                request.getImageUrl()
        );
        productRepository.save(product);

        // Tạo tồn kho ban đầu liên kết với vị trí kho mặc định (LOC-A1)
        StorageLocation location = getDefaultLocation();
        Inventory inventory = Inventory.createFor(product, location, 0);
        inventoryRepository.save(inventory);

        return toDetailDto(product);
    }

    @Override
    public ProductDetailDto updateProduct(Long id, ProductSaveRequest request) {
        Product product = findProductOrThrow(id);
        validateBarcodeUnique(request.getBarcode(), id);

        Category category = resolveCategory(request.getCategoryId());
        product.updateDetails(
                category,
                request.getName(),
                request.getBarcode(),
                request.getPrice(),
                request.getImageUrl(),
                request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE
        );
        productRepository.save(product);
        return toDetailDto(product);
    }

    @Override
    public void deactivateProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.deactivate();
        productRepository.save(product);
    }

    @Override
    public void updateInventory(Long productId, InventoryAdjustRequest request) {
        Product product = findProductOrThrow(productId);
        product.setReorderLevel(request.getLowStockThreshold());
        productRepository.save(product);

        Inventory inventory = inventoryRepository.findFirstByProductId(productId).orElse(null);
        if (inventory == null) {
            StorageLocation location = getDefaultLocation();
            inventory = Inventory.createFor(product, location, request.getNewQuantity());
        } else {
            inventory.setQuantity(request.getNewQuantity());
        }
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSearchResultDto> searchForPos(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Pageable limit = PageRequest.of(0, 20);
        return productRepository
                .searchActiveByNameOrBarcode(query.trim(), limit)
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId).orElse(null);
    }

    private StorageLocation getDefaultLocation() {
        return storageLocationRepository.findByCode("LOC-A1")
                .or(() -> storageLocationRepository.findFirstByOrderByIdAsc())
                .orElse(null);
    }

    private void validateBarcodeUnique(String barcode, Long excludeProductId) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        boolean duplicate = (excludeProductId == null)
                ? productRepository.existsByBarcode(barcode)
                : productRepository.existsByBarcodeAndIdNot(barcode, excludeProductId);

        if (duplicate) {
            throw new IllegalArgumentException("Barcode '" + barcode + "' đã tồn tại trong hệ thống.");
        }
    }

    private ProductDetailDto toDetailDto(Product p) {
        Integer totalQty = inventoryRepository.sumQuantityOnHandByProductId(p.getId());
        return new ProductDetailDto(
                p.getId(),
                p.getName(),
                p.getBarcode(),
                p.getPrice(),
                p.getImageUrl(),
                p.getStatus(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                totalQty != null ? totalQty : 0,
                p.getReorderLevel(),
                p.getSku()
        );
    }

    private ProductSearchResultDto toSearchResult(Product p) {
        Integer totalQty = inventoryRepository.sumQuantityOnHandByProductId(p.getId());
        return new ProductSearchResultDto(
                p.getId(),
                p.getName(),
                p.getBarcode(),
                p.getPrice(),
                totalQty != null ? totalQty : 0,
                p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getName() : null
        );
    }
}