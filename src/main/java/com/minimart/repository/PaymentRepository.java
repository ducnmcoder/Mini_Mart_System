package com.minimart.repository;

import com.minimart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleId(Long saleId);

    Optional<Payment> findFirstBySaleIdOrderByPaidAtDesc(Long saleId);
}