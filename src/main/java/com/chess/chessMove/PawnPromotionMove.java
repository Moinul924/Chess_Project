package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;

public class PawnPromotionMove extends Move {

    private Piece promotedPiece;

    public PawnPromotionMove(BoardSquare start, BoardSquare end, Piece piece, Piece promotedPiece) {
        super(start, end, piece);
        this.promotedPiece = promotedPiece;
    }

    public Piece getPromotedPiece() {
        return promotedPiece;
    }


    public void RemovePriveousMoveFromMoveHistory(Board board){
        if(board.moveHistory.size() > 0){
            Move LastMove = board.moveHistory.getLast();
            if(LastMove.pieceMoved.getName().equals("Pawn") && LastMove.startSquare == this.startSquare){
                pieceCaptured = LastMove.pieceCaptured;
                board.moveHistory.remove(board.moveHistory.size() - 1);
            }
            
        }
    }

    @Override
    public void execute(Board board) {
        startSquare.removePiece();
        if (endSquare.isOccupied()) {
            endSquare.removePiece();
        }
        endSquare.addPiece(promotedPiece);
        RemovePriveousMoveFromMoveHistory(board);
        board.moveHistory.add(this);
        board.currentWhiteTurn = !board.currentWhiteTurn;
    }

    @Override
    public void undo(Board board) {
        performNormalUndo(board);
    }
    
}
