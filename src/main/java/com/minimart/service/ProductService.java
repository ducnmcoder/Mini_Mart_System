package com.minimart.service;

import com.minimart.dto.request.InventoryAdjustRequest;
import com.minimart.dto.request.ProductSaveRequest;
import com.minimart.dto.response.ProductDetailDto;
import com.minimart.dto.response.ProductSearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /** Lay danh sach san pham (co phan trang va tim kiem) cho Owner */
    Page<ProductDetailDto> getProducts(String keyword, Pageable pageable);

    /** Lay thong tin chi tiet mot san pham */
    ProductDetailDto getProductById(Long id);

    /** Tao moi san pham */
    ProductDetailDto createProduct(ProductSaveRequest request);

    /** Cap nhat san pham */
    ProductDetailDto updateProduct(Long id, ProductSaveRequest request);

    /** Vo hieu hoa san pham (soft delete) */
    void deactivateProduct(Long id);

    /** Cap nhat ton kho */
    void updateInventory(Long productId, InventoryAdjustRequest request);

    /** Tim san pham cho man hinh POS, tra ve ket qua nho gon */
    List<ProductSearchResultDto> searchForPos(String query);

    /** Lay danh sach danh muc */
    List<com.minimart.entity.Category> getAllCategories();
}
