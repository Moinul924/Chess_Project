package com.chess.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class AccountService {
    private final AccountRepository accounts;
    private final PlayerProfileRepository profiles;
    private final UserSettingsRepository settings;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts,PlayerProfileRepository profiles,
                        UserSettingsRepository settings,PasswordEncoder passwordEncoder){
        this.accounts = accounts;
        this.profiles = profiles;
        this.settings = settings;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional 
    public Account register(RegisterRequest request){
        if(!request.confirmPassword().equals(request.password())){
            throw new IllegalArgumentException("Passwords do not match");
        }
        if(accounts.existsByUsername(request.username())){
            throw new IllegalArgumentException("Username already exists");
        }
        if(accounts.existsByEmail(request.email())){
            throw new IllegalArgumentException("Email already exists");
        }

        Account account = new Account();
        account.setUsername(request.username());
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));

        Account saved = accounts.save(account);

        PlayerProfile profile = new PlayerProfile();
        profile.setAccountId(saved.getId());
        profile.setDisplayName(request.username());
        profiles.save(profile);
      
        UserSettings userSettings = new UserSettings();
        userSettings.setAccountId(saved.getId());
        settings.save(userSettings);

        return saved;
    }

}
