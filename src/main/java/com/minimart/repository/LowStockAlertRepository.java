package com.minimart.repository;

import com.minimart.entity.LowStockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    boolean existsByProductIdAndStatus(Long productId, String status);
}