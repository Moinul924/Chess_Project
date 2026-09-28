package com.chess.account;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api-account")
public class AccountController {


    private final AccountService accountService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    
    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @PostMapping ("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        try{
            accountService.register(request);
            return ResponseEntity.status(201).body("User registered successfully");
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request,HttpServletRequest httpRequest,HttpServletResponse httpResponse) {
        
        try {
            Account account = accountService.login(request);

            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(account.getUsername(), null, List.of());

            // 2. Put it in a fresh SecurityContext and make it current
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 3. New session id at login time (prevents session fixation)
            httpRequest.getSession();
            httpRequest.changeSessionId();

            // 4. Save it into the session so the NEXT request still knows who this is
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return ResponseEntity.ok(account.getUsername());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/me")
    public ResponseEntity<String> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(principal.getName());
    }

}

    