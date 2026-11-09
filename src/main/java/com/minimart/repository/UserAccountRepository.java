package com.minimart.repository;

import com.minimart.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByAccountNameIgnoreCase(String accountName);

    boolean existsByAccountNameIgnoreCaseAndIdNot(String accountName, Long id);

    Optional<UserAccount> findByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<UserAccount> findByDeletedAtIsNullOrderByIdAsc();

    long countByRoleAndStatusAndDeletedAtIsNull(com.minimart.entity.Role role, com.minimart.entity.AccountStatus status);
}
