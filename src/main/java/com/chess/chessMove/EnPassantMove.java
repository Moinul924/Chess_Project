package com.chess.chessMove;

import com.chess.Board;
import com.chess.BoardSquare;
import com.chess.chessPiece.*;;

public class EnPassantMove extends Move {
    private BoardSquare capturedPawnSquare;

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
        board.removePiece(capturedPawnSquare);
        board.moveHistory.add(this);
    }

    @Override
    public void undo(Board board) {
        board.removePiece(endSquare);
        board.addPiece(pieceMoved, startSquare);
        board.addPiece(pieceCaptured, capturedPawnSquare); // Put the pawn back exactly where it was
        UndoBoardStates(board);
        board.currentWhiteTurn = !board.currentWhiteTurn;
    }
}
