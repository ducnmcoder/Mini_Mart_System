package com.minimart.repository;

import com.minimart.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    Optional<StorageLocation> findFirstByOrderByIdAsc();
    Optional<StorageLocation> findByCode(String code);
}