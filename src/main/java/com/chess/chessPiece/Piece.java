package com.chess.chessPiece;

import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessMove.Move;


public abstract class Piece {
    private PieceColour colour;
    private String name; 

    
    protected boolean isPiecePinned = false;
    protected int[][] pinDirection = null; 

    public Piece(PieceColour colour, String name) {
        this.colour = colour;
        this.name = name;
    }

    public PieceColour getColour() { return colour; }
    public String getName() { return name; }

    public void setPinDirection(int[][] directionsPiecePinedIn) {
        this.isPiecePinned = true;
        this.pinDirection = directionsPiecePinedIn;
    }


    public void removePin(Board board) {
        board.currentlyPinnedPieces.remove(this);
        this.isPiecePinned = false;
        this.pinDirection = null;
    }

    protected boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    protected boolean isMoveInPinDirection(int[] moveOffset, int[][] pinDirection) {
        for (int[] direction : pinDirection) {
            if (direction[0] == moveOffset[0] && direction[1] == moveOffset[1]) {
                return true;
            } 
        }
        return false;
    }

    public abstract int[][] getMoveOffsets(); 
    public abstract List<Move> getLegalMoves(BoardSquare currentSquare, Board board);  
}