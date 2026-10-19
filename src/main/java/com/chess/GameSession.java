package com.chess;

import com.chess.chessPiece.PieceColour;

// ENGINE = one account against the bot
// LOCAL  = one account playing both sides on one screen (pass and play)
// ONLINE = two different accounts, each in their own browser
enum GameMode { ENGINE, LOCAL, ONLINE }

class GameSession {
    Board gameBoard;
    Engine engine;
    BoardSquare selectedSquare;
    BoardSquare targetSquare;

    final GameMode mode;
    final Long ownerAccountId;   // the account that created the game

    // Who sits in each seat. null means the bot (ENGINE) or "nobody has joined yet" (ONLINE).
    // In LOCAL games the owner sits in both seats.
    Long whiteAccountId;
    Long blackAccountId;

    // Goes up every time the board changes, so the other player's browser can tell
    // "something happened" by comparing one number instead of the whole board.
    int version;

    // Set once the finished game has been saved to the database, so it's never saved twice.
    boolean recorded;

    GameSession(GameMode mode, Long ownerAccountId, PieceColour ownerColour) {
        this.mode = mode;
        this.ownerAccountId = ownerAccountId;
        switch (mode) {
            case LOCAL -> {
                whiteAccountId = ownerAccountId;
                blackAccountId = ownerAccountId;
            }
            case ENGINE -> {
                if (ownerColour == PieceColour.BLACK) blackAccountId = ownerAccountId;
                else whiteAccountId = ownerAccountId;
            }
            case ONLINE -> whiteAccountId = ownerAccountId;   // the second player joins as Black
        }
        startNewGame();
    }

    void startNewGame() {
        selectedSquare = null;
        targetSquare = null;
        gameBoard = new Board();
        gameBoard.initialisePieces();
        engine = new Engine(gameBoard);
        recorded = false;
        version++;
    }

    boolean isPlayer(Long accountId) {
        return accountId.equals(ownerAccountId)
            || accountId.equals(whiteAccountId)
            || accountId.equals(blackAccountId);
    }

    // The account whose turn it is, or null when it's the bot's turn / an empty seat.
    Long accountToMove() {
        return gameBoard.currentWhiteTurn ? whiteAccountId : blackAccountId;
    }

    PieceColour colourOf(Long accountId) {
        if (accountId.equals(whiteAccountId)) return PieceColour.WHITE;
        if (accountId.equals(blackAccountId)) return PieceColour.BLACK;
        return null;
    }
}
