package com.chess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                // TEMPORARY: everything else is open until Phase 2 (real login/registration)
                // replaces this with actual authorizeHttpRequests rules.
                .anyRequest().permitAll())
            // The H2 console submits its own forms without a CSRF token, so it needs an exemption.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            // The H2 console renders itself inside an <iframe> from the same origin,
            // so same-origin framing needs to be allowed (the default is DENY).
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .build();
    }

    @Bean 
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
