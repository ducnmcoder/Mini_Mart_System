package com.minimart.security;

import com.minimart.entity.UserAccount;
import com.minimart.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MiniMartOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final UserAccountRepository accounts;
    private final OidcUserService delegate = new OidcUserService();

    public MiniMartOidcUserService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        return localUser(delegate.loadUser(request));
    }

    OidcUser localUser(OidcUser googleUser) {
        if (!Boolean.TRUE.equals(googleUser.getEmailVerified()) || googleUser.getEmail() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_unverified"), "Email Google chưa được xác minh.");
        }
        UserAccount account = accounts.findByEmailIgnoreCase(googleUser.getEmail()).orElseThrow(
                () -> new OAuth2AuthenticationException(new OAuth2Error("account_not_found"), "Tài khoản email này không tồn tại."));
        if (!account.isLoginAllowed()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_inactive"), "Tài khoản đã ngừng hoạt động.");
        }
        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().getName().name())),
                googleUser.getIdToken(), googleUser.getUserInfo(), "email");
    }
}
