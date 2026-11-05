package com.minimart.repository;

import com.minimart.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByCashierIdAndStatusOrderBySoldAtDesc(Long cashierId,
                                                         String status,
                                                         Pageable pageable);

    @Query(value = "SELECT s FROM Sale s " +
                   "WHERE (:cashierId IS NULL OR s.cashierId = :cashierId) " +
                   "AND s.status = 'COMPLETED' " +
                   "AND (:fromDate IS NULL OR s.soldAt >= :fromDate) " +
                   "AND (:toDate IS NULL OR s.soldAt <= :toDate) " +
                   "AND (:keyword IS NULL OR LOWER(s.saleNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "     OR STR(s.id) LIKE CONCAT('%', :keyword, '%') " +
                   "     OR EXISTS (SELECT 1 FROM SaleItem si JOIN si.product p WHERE si.sale = s AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
                   "ORDER BY s.soldAt DESC",
           countQuery = "SELECT count(s) FROM Sale s " +
                   "WHERE (:cashierId IS NULL OR s.cashierId = :cashierId) " +
                   "AND s.status = 'COMPLETED' " +
                   "AND (:fromDate IS NULL OR s.soldAt >= :fromDate) " +
                   "AND (:toDate IS NULL OR s.soldAt <= :toDate) " +
                   "AND (:keyword IS NULL OR LOWER(s.saleNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "     OR STR(s.id) LIKE CONCAT('%', :keyword, '%') " +
                   "     OR EXISTS (SELECT 1 FROM SaleItem si JOIN si.product p WHERE si.sale = s AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Sale> filterSales(@Param("cashierId") Long cashierId,
                           @Param("keyword") String keyword,
                           @Param("fromDate") LocalDateTime fromDate,
                           @Param("toDate") LocalDateTime toDate,
                           Pageable pageable);
}