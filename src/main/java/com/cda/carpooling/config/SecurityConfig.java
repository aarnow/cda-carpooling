package com.cda.carpooling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Désactiver CSRF pour l'API REST
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/api/**").permitAll() // Autoriser ces endpoints
                        .anyRequest().authenticated() // Les autres nécessitent une authentification
                );

        return http.build();
    }
}