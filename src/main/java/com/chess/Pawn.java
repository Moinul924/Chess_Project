package com.chess;
import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    
    private int[][] whiteOffsets = { {1,0}, {2,0}, {1,1}, {1,-1} };
    private int[][] blackOffsets = { {-1,0}, {-2,0}, {-1,1}, {-1,-1} };

    public Pawn(PieceColour PawnColour) {
        super(PawnColour, "Pawn");
    }

    @Override
    List<BoardSquare> getLegalMoves(BoardSquare currentSquare, Board board) {   
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        } 
        int currentRow = currentSquare.getRow();
        int currentCol = currentSquare.getCol(); 
        List<BoardSquare> legalMoves = new ArrayList<>(); 

        int[][] activeOffsets = (getColour() == PieceColour.WHITE) ? whiteOffsets : blackOffsets;

        for (int[] offset : activeOffsets) {
            
            if (isPiecePinned) {
                if (!isMoveInPinDirection(offset, pinDirection)) {
                    continue; 
                }
            }
            int newRow = currentRow + offset[0];
            int newCol = currentCol + offset[1];
            if (IsMoveLegal(currentRow, currentCol, newRow, newCol, board)) {
                legalMoves.add(board.getSquare(newRow, newCol));
            }
        }
        
        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move));
        }

        return legalMoves;
    }

    private boolean IsMoveLegal(int startRow, int startCol, int endRow, int endCol, Board board) {
        if (!isWithinBounds(endRow, endCol)) {
            return false;
        }

        int direction = (getColour() == PieceColour.WHITE) ? 1 : -1;
        int PawnstartRow = (getColour() == PieceColour.WHITE) ? 1 : 6;

        int deltaRow = endRow - startRow; 
        int deltaCol = Math.abs(endCol - startCol); 

        if (deltaCol == 0 && deltaRow == direction) {
            return !board.isSquareOccupied(endRow, endCol);
        }
        if (deltaCol == 0 && deltaRow == direction * 2 && startRow == PawnstartRow) {
            boolean isIntermediateSquareEmpty = !board.isSquareOccupied(startRow + direction, startCol );
            boolean isDestinationEmpty = !board.isSquareOccupied(endRow, endCol);
            return isIntermediateSquareEmpty && isDestinationEmpty;
        }

        if (deltaCol == 1 && deltaRow == direction) {
            if (board.isSquareOccupied(endRow, endCol)) {
                return board.getSquare(endRow, endCol).getPiece().getColour() != getColour();
            }
        }

        return false;
    }


    @Override
    public int[][] getMoveOffsets() {
        return new int[][] { 
            {1,0}, {2,0}, {1,1}, {1,-1},
            {-1,0}, {-2,0}, {-1,1}, {-1,-1} 
        };
    }

    
}
