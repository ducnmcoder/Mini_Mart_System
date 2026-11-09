package com.minimart.security;

import com.minimart.repository.UserAccountRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(MiniMartUserDetailsService userDetailsService,
                                                     PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RoleLoginSuccessHandler successHandler,
                                            LoginFailureHandler failureHandler,
                                            GoogleLoginFailureHandler googleFailureHandler,
                                            MiniMartOidcUserService oidcUserService,
                                            ObjectProvider<ClientRegistrationRepository> googleRegistration,
                                            UserAccountRepository accountRepository,
                                            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/oauth2/**", "/login/oauth2/**", "/forgot-password", "/forgot-password/reset", "/css/**", "/js/**", "/favicon.svg").permitAll()
                        .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers("/admin/accounts/**", "/admin/accounts").hasRole("ADMIN")
                        .requestMatchers("/dashboard/owner").hasRole("OWNER")
                        .requestMatchers("/dashboard/sale-staff").hasRole("SALE_STAFF")
                        .requestMatchers("/dashboard/inventory-staff").hasRole("INVENTORY_STAFF")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("accountName")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/dashboard"))
                .addFilterBefore(new AccountSessionFilter(accountRepository), AuthorizationFilter.class);

        ClientRegistrationRepository registration = googleRegistration.getIfAvailable();
        if (registration != null) {
            var resolver = new DefaultOAuth2AuthorizationRequestResolver(registration, "/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(builder ->
                    builder.additionalParameters(parameters -> parameters.put("prompt", "select_account")));
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                    .successHandler(successHandler)
                    .failureHandler(googleFailureHandler));
        }

        return http.build();
    }
}
