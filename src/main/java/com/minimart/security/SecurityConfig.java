package com.minimart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                                            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/sale/**", "/owner/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.svg").permitAll()
                        .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                        .requestMatchers("/dashboard/owner").hasRole("OWNER")
                        .requestMatchers("/dashboard/sale-staff").hasRole("SALE_STAFF")
                        .requestMatchers("/dashboard/inventory-staff").hasRole("INVENTORY_STAFF")
                        .requestMatchers("/owner/products/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/sale/**").hasAnyRole("SALE_STAFF", "OWNER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    String accept = request.getHeader("Accept");
                    String requestedWith = request.getHeader("X-Requested-With");
                    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"))) {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"Bạn không có quyền thực hiện thao tác này\"}");
                    } else {
                        response.sendRedirect("/dashboard");
                    }
                }));

        return http.build();
    }
}
