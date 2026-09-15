package com.chess;

class GameSession {
    Board gameBoard;
    Engine engine;
    BoardSquare selectedSquare;
    BoardSquare targetSquare;

    GameSession() {
        startNewGame();
    }

    void startNewGame() {
        selectedSquare = null;
        targetSquare = null;
        gameBoard = new Board();
        gameBoard.initialisePieces();
        engine = new Engine(gameBoard);
    }
}
