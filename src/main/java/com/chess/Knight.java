package com.chess;
import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    private int[][] moveOffsets = {
            {2,1},{2,-1},{-2,1},{-2,-1},
            {1,2},{1,-2},{-1,2},{-1,-2}
        };

    Knight(PieceColour KnightColour) {
        super(KnightColour, "Knight");
    }

    @Override
    List<BoardSquare> getLegalMoves(BoardSquare currentSquare , Board board) {
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        } 

        List<BoardSquare> legalMoves = new ArrayList<>();
        
        int CurrentRow = currentSquare.getRow();
        int CurrentCol = currentSquare.getCol();   
        
        for (int[] offset : moveOffsets) {
            if(isPiecePinned) {
                if (!isMoveInPinDirection(offset,pinDirection)) {
                    continue; 
                }
            }
            int newRow = CurrentRow + offset[0];
            int newCol = CurrentCol + offset[1];
            if (isItValidSquare(newRow, newCol, board)) {
                legalMoves.add(board.getSquare(newRow, newCol));
            }
        }
        
        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move));
        }

        return legalMoves;
    }


    private boolean isItValidSquare(int endRow, int endCol, Board board){

        if(!isWithinBounds(endRow, endCol)){
            return false;
        }

        if(board.isSquareOccupied(endRow, endCol)){
            return getColour() != board.getSquare(endRow,endCol).getPiece().getColour(); 
        }
		return true;

    }

    @Override
    public int[][] getMoveOffsets() {
        return moveOffsets;
    }

    
}
