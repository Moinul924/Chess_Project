package com.chess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.chess.MatchmakingQueue;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, MatchmakingQueue matchmaking) throws Exception {
        return http
            // Rules are checked top to bottom and the FIRST match wins,
            // so the public exceptions must come before the catch-all at the end.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                // Pages and files anyone may load without an account
                .requestMatchers("/", "/index.html", "/singInOrRegister.html",
                                 "/css/**", "/js/**", "/piece_images/**", "/chess_sounds/**",
                                 "/favicon.ico").permitAll()
                // The API calls needed to get an account in the first place
                .requestMatchers("/api/csrf", "/api-account/register", "/api-account/login").permitAll()
                // Spring's error page: if this were protected, a real 404 or 500 would show up as a 401
                .requestMatchers("/error").permitAll()
                // Everything else (play.html, the whole game API, /me, logout) needs a login
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(notLoggedIn()))
            // By default, Spring saves every refused request in a NEW session so it can send you back
            // there after logging in. This app never uses that, and it caused a real bug: on the sign-in
            // page, the navbar's /me check and the CSRF-token request both created a session at the same
            // moment, the browser kept whichever cookie arrived last, and register/login then failed with 403.
            .requestCache(cache -> cache.disable())
            // The H2 console submits its own forms without a CSRF token, so it needs an exemption.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            // The H2 console renders itself inside an <iframe> from the same origin,
            // so same-origin framing needs to be allowed (the default is DENY).
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .logout(logout -> logout
                .logoutUrl("/api-account/logout")
                // Logging out while searching for a game takes you out of the queue straight away,
                // so nobody gets paired with an account that is no longer there.
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null && authentication.getPrincipal() instanceof Long accountId) {
                        matchmaking.leave(accountId);
                    }
                })
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
            .build();
    }

    // What happens when someone who isn't logged in asks for a protected URL.
    // A browser opening a page (e.g. typing /play.html) is sent to the sign-in page;
    // a fetch() call from JavaScript gets a plain 401 so the script can decide what to do.
    private AuthenticationEntryPoint notLoggedIn() {
        return (request, response, authException) -> {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html")) {
                response.sendRedirect("/singInOrRegister.html");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
