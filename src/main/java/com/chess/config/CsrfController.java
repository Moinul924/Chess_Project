package com.chess.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    // The frontend calls this once on page load, then sends the token on every POST.
    // Spring injects the CsrfToken for the current session automatically.
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
