package com.minimart.repository;

import com.minimart.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findFirstByProductId(Long productId);

    @Query("SELECT COALESCE(SUM(i.quantityOnHand), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer sumQuantityOnHandByProductId(@Param("productId") Long productId);
}