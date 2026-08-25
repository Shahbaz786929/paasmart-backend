package com.paasmart.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Autowired private JwtAuthFilter jwtAuthFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/docs", "/docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login-otp", "/api/v1/auth/verify-otp", "/api/v1/auth/google", "/api/v1/auth/google-config", "/api/v1/auth/email-register", "/api/v1/auth/email-register-verify", "/api/v1/auth/email-login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/web-push/vapid-public-key").permitAll()
                        .requestMatchers("/api/web-push/**").authenticated()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/tryon/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/returns/**").authenticated()
                        .requestMatchers("/api/v1/products/**").permitAll()
                        .requestMatchers("/api/v1/tenants").permitAll()
                        .requestMatchers("/api/v1/super-admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/deals/**").permitAll()
                        .requestMatchers("/api/v1/seller/flash-sales/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/**").permitAll()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/cart/**", "/api/address/**", "/api/wishlist/**", "/api/checkout/**", "/api/wallet/**", "/api/coupons/apply", "/api/notifications/**").authenticated()
                        .requestMatchers("/api/v1/seller/register").hasRole("SELLER")
                        .requestMatchers("/api/delivery/**").hasRole("DELIVERY")
                        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "TENANT_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder () {
        return new BCryptPasswordEncoder();
    }
}