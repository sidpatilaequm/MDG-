package com.example.multimedia.file_upload_api.config;

import com.example.multimedia.file_upload_api.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers("/api/super-admin/**").permitAll()
                        .requestMatchers("/api/v1/bom/master/**").hasAuthority("super_admin")
                        .requestMatchers("/api/business-card/**").permitAll()
                        .requestMatchers("/api/ocr/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        // One-click email approve/reject link - permitAll at this layer; the
                        // controller itself validates the action-token JWT (its own secret,
                        // separate from login sessions) before doing anything.
                        .requestMatchers("/api/approvals/action/**").permitAll()
                        .requestMatchers("/api/actuator/health", "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/approver-groups/**").hasAuthority("super_admin")
                        .requestMatchers("/api/supplier-registration/**", "/api/approvals/**")
                            .hasAnyAuthority("approver", "super_admin")
                        // Deliberately NOT "/api/vendor/**" - VendorCatalogueController already
                        // lives at that prefix for any authenticated user; a new, narrower path
                        // avoids tightening that existing, unrelated endpoint's access rule.
                        .requestMatchers("/api/vendor-change-requests/**").hasAnyAuthority("vendor", "super_admin")
                        .requestMatchers("/api/company-addresses/**").authenticated()
                        .requestMatchers("/api/upload/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}