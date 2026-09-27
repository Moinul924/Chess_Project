package com.chess.account;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity                       // "this class maps to a database table"
@Table(name = "account")      // ...and this is the table's name
public class Account {

    @Id                                                   // this field is the PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database numbers it (1, 2, 3...)
    private Long id;

    private String username;  
    private String email;     
    private String passwordHash;  
    private boolean enabled = true;       
    private Instant createdAt = Instant.now();  // default to the current time

    public Account() {
        // JPA requires a no-argument constructor
    }
    public Long getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }

    


}
