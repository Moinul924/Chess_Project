package com.chess.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "player_profile")
public class PlayerProfile {
    @Id 
    private Long accountId;  // same as the Account's ID; one-to-one relationship
    private String displayName;
    private String country; 
    private int rating = 1200;
    private int peakRating = 1200;
    private int gamesPlayed;
    private int wins;
    private int draws;
    private int losses;

    public PlayerProfile() {
        // JPA requires a no-argument constructor
    }
    public Long getAccountId() {
        return accountId;
    }
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public int getRating() {
        return rating;
    }
    public void setRating(int rating) {
        this.rating = rating;
    }
    public int getPeakRating() {
        return peakRating;
    }
    public void setPeakRating(int peakRating) {
        this.peakRating = peakRating;
    }
    public int getGamesPlayed() {
        return gamesPlayed;
    }
    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
    public int getWins() {
        return wins;
    }
    public void setWins(int wins) {
        this.wins = wins;
    }
    public int getDraws() {
        return draws;
    }
    public void setDraws(int draws) {
        this.draws = draws;
    }
    public int getLosses() {
        return losses;
    }
    public void setLosses(int losses) {
        this.losses = losses;
    }
    
}
