package com.chess.account;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.chess.MatchmakingQueue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api-account")
public class AccountController {


    private final AccountService accountService;
    private final MatchmakingQueue matchmaking;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AccountController(AccountService accountService, MatchmakingQueue matchmaking){
        this.accountService = accountService;
        this.matchmaking = matchmaking;
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
            Account account = accountService.login(request); ///check if the login is valid and get the account details

            // Every tab in a browser shares ONE login. If this browser was already signed in as a different
            // account (say, searching for a game in another tab), that account is now gone from this browser,
            // so take it out of the matchmaking queue before someone gets paired with it.
            Authentication previous = SecurityContextHolder.getContext().getAuthentication();
            if (previous != null && previous.getPrincipal() instanceof Long previousId && !previousId.equals(account.getId())) {
                matchmaking.leave(previousId);
            }

            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(account.getId(), null, List.of());

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
    public ResponseEntity<String> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        Long accountId = (Long) authentication.getPrincipal();

        Account loggedInAccount = accountService.getAccountById(accountId);

        return ResponseEntity.ok(String.valueOf(loggedInAccount.getUsername()));
    }

    // The logged-in player's settings, e.g. { "boardTheme": "red-cream" }
    @GetMapping("/settings")
    public SettingsData getSettings(Authentication authentication) {
        return accountService.getSettings((Long) authentication.getPrincipal());
    }

    // The Save button on the settings page.
    @PostMapping("/settings")
    public ResponseEntity<String> saveSettings(@Valid @RequestBody SettingsData request, Authentication authentication) {
        try {
            accountService.updateBoardTheme((Long) authentication.getPrincipal(), request.boardTheme());
            return ResponseEntity.ok("Settings saved");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}

    