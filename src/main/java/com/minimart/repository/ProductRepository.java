package com.minimart.repository;

import com.minimart.entity.Product;
import com.minimart.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Tim theo keyword (ten hoac barcode) va trang thai, dung cho trang quan ly Owner */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category " +
           "WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR p.barcode LIKE CONCAT('%', :keyword, '%')) " +
           "AND p.status = :status")
    Page<Product> findByKeywordAndStatus(@Param("keyword") String keyword,
                                         @Param("status") ProductStatus status,
                                         Pageable pageable);

    /** Tim san pham cho POS (ten hoac barcode), chi lay ACTIVE */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category " +
           "WHERE p.status = com.minimart.entity.ProductStatus.ACTIVE AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           " OR p.barcode LIKE CONCAT('%', :query, '%'))")
    Page<Product> searchActiveByNameOrBarcode(@Param("query") String query, Pageable pageable);

    /** Kiem tra barcode trung, bo qua san pham dang sua */
    boolean existsByBarcodeAndIdNot(String barcode, Long excludeId);

    boolean existsByBarcode(String barcode);

    Optional<Product> findByBarcodeAndStatus(String barcode, ProductStatus status);
}
