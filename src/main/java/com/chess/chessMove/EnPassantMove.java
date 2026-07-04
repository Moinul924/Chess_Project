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
        capturedPawnSquare.removePiece(board);
        board.moveHistory.add(this);
    }

    @Override
    public void undo(Board board) {
        endSquare.removePiece(board);
        startSquare.addPiece(pieceMoved, board);
        capturedPawnSquare.addPiece(pieceCaptured, board); // Put the pawn back exactly where it was
        UndoBoardStates(board);
        board.currentWhiteTurn = !board.currentWhiteTurn;
    }
}
