package com.github.popularityscore.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


    @Configuration
    @Profile("keycloak")
    @EnableWebSecurity
    @ConditionalOnProperty(value = "app.security.enabled", havingValue = "true")
    public class SecurityEnabledConfig {
        @Bean
        SecurityFilterChain secured(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/swagger-ui/**","/v3/api-docs/**", "/").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(o -> o.jwt()); // ✅ only JWT validation
            return http.build();
        }
    }
