package com.chess.chessPiece;

import java.util.ArrayList;
import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessMove.Move;



public class Knight extends Piece {

    protected int[][] moveOffsets = {
            {2,1},{2,-1},{-2,1},{-2,-1},
            {1,2},{1,-2},{-1,2},{-1,-2}
        };

    public Knight(PieceColour KnightColour) {
        super(KnightColour, "Knight");
        pieceValue = 300;
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

    
    @Override
    public List<Move> getLegalMoves(BoardSquare currentSquare , Board board) {
        if (board.currentWhiteTurn && getColour() == PieceColour.BLACK || !board.currentWhiteTurn && getColour() == PieceColour.WHITE) {
            return new ArrayList<>();
        } 
    
        List<Move> legalMoves = new ArrayList<>();
        
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
                BoardSquare targetSquare = board.getSquare(newRow, newCol);
                Move move = new Move(currentSquare, targetSquare, this);
                legalMoves.add(move);
            }
        }
        
        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move.getEndSquare()));
        }
    
        return legalMoves;
    }
}
