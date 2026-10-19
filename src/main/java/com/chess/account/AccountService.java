package com.chess.account;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class AccountService {
    private final AccountRepository accounts;
    private final PlayerProfileRepository profiles;
    private final UserSettingsRepository settings;
    private final PasswordEncoder passwordEncoder;

    // The board themes the settings page offers. The server only accepts these, so nobody can save
    // a made-up value. Keep in sync with the keys in static/js/themes.js (which holds the colours).
    public static final Set<String> BOARD_THEMES = Set.of(
        "classic", "black-white", "grey-white", "carbon-antique",
        "oat-sand", "red-cream", "pink-blue", "forest");


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

    @Transactional(readOnly = true)
    public Account login(LoginRequest request){
        String login = request.usernameOrEmail();
        Optional<Account> found = login.contains("@") ? accounts.findByEmail(login): accounts.findByUsername(login);

        Account account = found.orElseThrow(
            () -> new IllegalArgumentException("Invalid username/email or password"));

        if(!passwordEncoder.matches(request.password(), account.getPasswordHash())){
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        return account;
    }

    public Account getAccountById(Long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    @Transactional(readOnly = true)
    public SettingsData getSettings(Long accountId) {
        String theme = settings.findById(accountId)
                .map(UserSettings::getBoardTheme)
                .orElse("classic");   // every account gets a settings row at registration; this is just a safe fallback
        return new SettingsData(theme);
    }

    @Transactional
    public void updateBoardTheme(Long accountId, String boardTheme) {
        if (!BOARD_THEMES.contains(boardTheme)) {
            throw new IllegalArgumentException("Unknown board theme");
        }
        UserSettings userSettings = settings.findById(accountId).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setAccountId(accountId);
            return created;
        });
        userSettings.setBoardTheme(boardTheme);
        settings.save(userSettings);   // UPDATE user_settings SET board_theme = ? WHERE account_id = ?
    }

    // The name shown on the board for a player, e.g. the opponent in an online game.
    @Transactional(readOnly = true)
    public String displayNameOf(Long accountId) {
        return profiles.findById(accountId)
                .map(PlayerProfile::getDisplayName)
                .orElse("Unknown player");
    }




}
