package com.chess.account;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table (name = "game_record")
public class GameRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long whiteAccountId;
    private Long blackAccountId;
    @Enumerated (EnumType.STRING)
    private OpponentType opponentType;
    @Enumerated (EnumType.STRING)
    private GameResult result;
    @Enumerated (EnumType.STRING)
    private GameTermination termination;
    private String moveListSan; 
    private String finalFen;
    private Integer whiteRatingBefore;
    private Integer whiteRatingAfter;
    private Integer blackRatingBefore;
    private Integer blackRatingAfter;
    private Instant playedAt = Instant.now();

    public GameRecord() {
        // JPA requires a no-argument constructor
    }
    public Long getId() {
        return id;
    }
    public Long getWhiteAccountId() {
        return whiteAccountId;
    }
    public void setWhiteAccountId(Long whiteAccountId) {
        this.whiteAccountId = whiteAccountId;
    }
    public Long getBlackAccountId() {
        return blackAccountId;
    }
    public void setBlackAccountId(Long blackAccountId) {
        this.blackAccountId = blackAccountId;
    }
    public OpponentType getOpponentType() {
        return opponentType;
    }
    public void setOpponentType(OpponentType opponentType) {
        this.opponentType = opponentType;
    }
    public GameResult getResult() {
        return result;
    }
    public void setResult(GameResult result) {
        this.result = result;
    }
    public GameTermination getTermination() {
        return termination;
    }
    public void setTermination(GameTermination termination) {
        this.termination = termination;
    }
    public String getMoveListSan() {
        return moveListSan;
    }
    public void setMoveListSan(String moveListSan) {
        this.moveListSan = moveListSan;
    }
    public String getFinalFen() {
        return finalFen;
    }
    public void setFinalFen(String finalFen) {
        this.finalFen = finalFen;
    }
    public Integer getWhiteRatingBefore() {
        return whiteRatingBefore;
    }
    public void setWhiteRatingBefore(Integer whiteRatingBefore) {
        this.whiteRatingBefore = whiteRatingBefore;
    }
    public Integer getWhiteRatingAfter() {
        return whiteRatingAfter;
    }
    public void setWhiteRatingAfter(Integer whiteRatingAfter) {
        this.whiteRatingAfter = whiteRatingAfter;
    }
    public Integer getBlackRatingBefore() {
        return blackRatingBefore;
    }
    public void setBlackRatingBefore(Integer blackRatingBefore) {
        this.blackRatingBefore = blackRatingBefore;
    }
    public Integer getBlackRatingAfter() {
        return blackRatingAfter;
    }
    public void setBlackRatingAfter(Integer blackRatingAfter) {
        this.blackRatingAfter = blackRatingAfter;
    }
    public Instant getPlayedAt() {
        return playedAt;
    }
}
