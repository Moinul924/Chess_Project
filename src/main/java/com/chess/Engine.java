package com.chess;

import java.util.ArrayList;
import java.util.List;

import com.chess.chessMove.Move;
import com.chess.chessPiece.PieceColour;

public class Engine {
    
    private Board Board;    
    public int Checkmate = 100000000;
    private Move BestMove;
    private int MaxDepth = 4;
    private PieceColour OriginalColour;
    int HighestDepth = 0;
    int PossitionEvaluated = 0;
    int numOfsimilarepossition = 0;
    List<Move> AllCurrentLegalMoves = new ArrayList<>();

    public Engine(Board board) {
        this.Board = board;
    }

    public void generateAllCurrentLegalMoves() {
        AllCurrentLegalMoves.clear();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                BoardSquare square = Board.getSquare(row, col);
                if (square.isOccupied() && ((Board.currentWhiteTurn && square.getPiece().getColour() == PieceColour.WHITE) || (!Board.currentWhiteTurn && square.getPiece().getColour() == PieceColour.BLACK))) {
                    AllCurrentLegalMoves.addAll(square.getPiece().getLegalMoves(square, Board, true));
                }
            }
        }
    }

}