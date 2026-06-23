package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;;

public class EnPassantMove extends Move {
    private BoardSquare capturedPawnSquare;
    private Piece pieceCaptured;// will this cause a bug 

    public EnPassantMove(BoardSquare start, BoardSquare end, Piece piece, BoardSquare capturedPawnSquare) {
        super(start, end, piece);
        this.capturedPawnSquare = capturedPawnSquare;
    }

    public BoardSquare getCapturedPawnSquare(){
        return capturedPawnSquare;
    }

    public Piece getPieceCaptured() {
        return pieceCaptured;
    }


    @Override
    public void execute(Board board) {
        performNormalMove(board);
        pieceCaptured = capturedPawnSquare.getPiece();
        capturedPawnSquare.removePiece();
        board.moveHistory.add(this);
    }

    @Override
    public void undo(Board board) {
        performNormalUndo(board);
        capturedPawnSquare.addPiece(this.pieceCaptured);
    }
}
