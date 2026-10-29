package com.minimart.security;

import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MiniMartUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accountRepository;

    public MiniMartUserDetailsService(UserAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = accountRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));

        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole().getName().name())
                .disabled(!account.isLoginAllowed())
                .build();
    }
}
