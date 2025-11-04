package com.github.popularityscore.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(value = "app.security.enabled", havingValue = "true")
public class SecurityEnabledConfig {
    @Bean
    SecurityFilterChain secured(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // IMPORTANT: don't enable httpBasic()
                .oauth2ResourceServer(oauth -> oauth.jwt())
                .oauth2Client(Customizer.withDefaults());

        return http.build();
    }
}
