package com.chess;
import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {
   
    private int[][] moveOffsets = {
            {1,0},{-1,0},{0,1},{0,-1},
            {1,1},{1,-1},{-1,1},{-1,-1}
        };

    Queen(PieceColour QueenColour) {
        super(QueenColour, "Queen");
    }

    @Override
    List<BoardSquare> getLegalMoves(BoardSquare currentSquare , Board board) {    
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        }
        int CurrentRow = currentSquare.getRow();
        int CurrentCol = currentSquare.getCol(); 
        List<BoardSquare> legalMoves = new ArrayList<>(); 
        for(int[] offset : moveOffsets){
            if(isPiecePinned) {
                if (!isMoveInPinDirection(offset,pinDirection)) {
                    continue; 
                }
            }
            int newRow = CurrentRow + offset[0];
            int newCol = CurrentCol + offset[1];
            while(isWithinBounds(newRow, newCol)){
                if(board.isSquareOccupied(newRow, newCol)){
                    if(getColour() != board.getSquare(newRow, newCol).getPiece().getColour()){
                        legalMoves.add(board.getSquare(newRow, newCol));
                    }
                    break;
                }
                legalMoves.add(board.getSquare(newRow, newCol));
                newRow += offset[0];
                newCol += offset[1];
            }
        }

        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move));
        }

        return legalMoves;
    }

    @Override
    public int[][] getMoveOffsets() {
        return moveOffsets;
    }

    
}
