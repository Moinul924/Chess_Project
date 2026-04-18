package com.chess.chessPiece;

import java.util.ArrayList;
import java.util.List;
import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessMove.Move;


public class Bishop extends Piece {

    protected int[][] moveOffsets = {
            {1,1},{1,-1},{-1,1},{-1,-1}
        };

    public Bishop(PieceColour BishopColour) {
        super(BishopColour, "Bishop");
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
        int CurrentRow = currentSquare.getRow();
        int CurrentCol = currentSquare.getCol(); 
        List<Move> legalMoves = new ArrayList<>(); 
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
                        BoardSquare targetSquare = board.getSquare(newRow, newCol);
                        Move move = new Move(currentSquare, targetSquare, this);
                        legalMoves.add(move);
                    }
                    break;
                }
                BoardSquare targetSquare = board.getSquare(newRow, newCol);
                Move move = new Move(currentSquare, targetSquare, this);
                legalMoves.add(move);
                newRow += offset[0];
                newCol += offset[1];
            }
        }
    
        if(board.KingInCheck){
            legalMoves.removeIf(move -> !board.squaresToBlockCheck.contains(move.getEndSquare()));
        }
    
        return legalMoves;
    }
    
}
