package com.chess.account;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api-account")
public class AccountController {


    private final AccountService accountService;
    
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
    
}