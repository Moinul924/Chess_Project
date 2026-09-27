package com.chess.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id 
    private Long accountId;  // same as the Account's ID; one-to-one relationship
    private String boardTheme = "classic";
    private boolean showLegalMoves = true;
    private int engineDepth = 4;

    public UserSettings() {
        // JPA requires a no-argument constructor
    }

    public Long getAccountId() {
        return accountId;
    }
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    public String getBoardTheme() {
        return boardTheme;
    }
    public void setBoardTheme(String boardTheme) {
        this.boardTheme = boardTheme;
    }
    public boolean isShowLegalMoves() {
        return showLegalMoves;
    }
    public void setShowLegalMoves(boolean showLegalMoves) {
        this.showLegalMoves = showLegalMoves;
    }
    public int getEngineDepth() {
        return engineDepth;
    }
    public void setEngineDepth(int engineDepth) {
        this.engineDepth = engineDepth;
    }
}
