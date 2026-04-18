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


    @Override
    public void execute(Board board) {
        performNormalMove(board);
        capturedPawnSquare.removePiece();
        board.moveHistory.add(this);
    }
    
}
