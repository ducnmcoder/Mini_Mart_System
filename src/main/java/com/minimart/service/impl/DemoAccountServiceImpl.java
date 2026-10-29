package com.minimart.service.impl;

import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import com.minimart.service.DemoAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DemoAccountServiceImpl implements DemoAccountService {

    private static final String PLACEHOLDER = "!SET_BCRYPT_HASH_VIA_APPLICATION!";

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountServiceImpl(UserAccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public int initializePlaceholderAccounts(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("APP_DEMO_PASSWORD phải có ít nhất 8 ký tự.");
        }

        List<UserAccount> accounts = accountRepository.findAll().stream()
                .filter(account -> PLACEHOLDER.equals(account.getPasswordHash()))
                .toList();
        accounts.forEach(account -> account.activateDemoAccount(passwordEncoder.encode(rawPassword)));
        accountRepository.saveAll(accounts);
        return accounts.size();
    }
}
