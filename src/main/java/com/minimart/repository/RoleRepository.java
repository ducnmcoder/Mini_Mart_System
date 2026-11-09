package com.minimart.repository;

import com.minimart.entity.AppRole;
import com.minimart.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.name = :name")
    Optional<Role> findWithLockByName(@Param("name") AppRole name);
}
